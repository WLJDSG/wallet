package com.zbkj.paychannel.core;

import com.zbkj.paychannel.enums.PayActionEnum;
import com.zbkj.paychannel.enums.PayErrorCode;
import com.zbkj.paychannel.enums.PayEventEnum;
import com.zbkj.paychannel.enums.PayStateEnum;
import com.zbkj.paychannel.enums.RefundEventEnum;
import com.zbkj.paychannel.enums.RefundStateEnum;
import com.zbkj.paychannel.exception.PayChannelException;
import com.zbkj.paychannel.machine.PayStateMachine;
import com.zbkj.paychannel.machine.RefundStateMachine;
import com.zbkj.paychannel.model.CallbackCommand;
import com.zbkj.paychannel.model.CallbackResult;
import com.zbkj.paychannel.model.CancelCommand;
import com.zbkj.paychannel.model.ChannelRefundCommand;
import com.zbkj.paychannel.model.ExecutePaymentCommand;
import com.zbkj.paychannel.model.ExecutePaymentResult;
import com.zbkj.paychannel.model.PayCommand;
import com.zbkj.paychannel.model.PayLogRecord;
import com.zbkj.paychannel.model.PayOrderSnapshot;
import com.zbkj.paychannel.model.PayResult;
import com.zbkj.paychannel.model.QueryCommand;
import com.zbkj.paychannel.model.QueryResult;
import com.zbkj.paychannel.model.RefundCommand;
import com.zbkj.paychannel.model.RefundResult;
import com.zbkj.paychannel.provider.CallbackProvider;
import com.zbkj.paychannel.provider.CancelProvider;
import com.zbkj.paychannel.provider.ExecutePaymentProvider;
import com.zbkj.paychannel.provider.PayProvider;
import com.zbkj.paychannel.provider.QueryProvider;
import com.zbkj.paychannel.provider.RefundProvider;
import com.zbkj.paychannel.spi.FeePolicy;
import com.zbkj.paychannel.spi.PayEventListener;
import com.zbkj.paychannel.spi.PayLockManager;
import com.zbkj.paychannel.spi.PayLogSink;
import com.zbkj.paychannel.spi.PayOrderRepository;
import com.zbkj.paychannel.spi.RefundOrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.function.Supplier;

/**
 * 支付编排器（对应 crmeb-pay-service 的 PaymentServiceImpl）。
 *
 * <p>并发与幂等模型：</p>
 * <ul>
 *   <li>同一订单的支付/取消各用一把锁，回调/查询/扣款确认共用一把结果锁（与原模块一致）；</li>
 *   <li>所有状态推进走仓储的条件更新（CAS），锁 + 状态机 canTransition + CAS 三重保证
 *       支付成功事件对同一交易至多发布一次；</li>
 *   <li>SDK 内不开数据库事务，渠道 HTTP 调用不在任何事务内（原模块的长事务问题在此消除），
 *       每次落库都是独立的短写入。</li>
 * </ul>
 */
public final class PaymentOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(PaymentOrchestrator.class);

    private static final String LOCK_PAY = "paychannel:pay:";
    private static final String LOCK_RESULT = "paychannel:result:";
    private static final String LOCK_REFUND = "paychannel:refund:";
    private static final String LOCK_CANCEL = "paychannel:cancel:";

    private final ProviderRegistry registry;
    private final PayOrderRepository payOrderRepository;
    private final RefundOrderRepository refundOrderRepository;
    private final PayLockManager lockManager;
    private final PayLogSink logSink;
    private final PayEventListener eventListener;
    private final FeePolicy feePolicy;

    /** 请通过 {@code PayChannelKernel.builder()} 组装，不要直接实例化（Kernel 负责注册表完整性校验） */
    public PaymentOrchestrator(ProviderRegistry registry, PayOrderRepository payOrderRepository,
        RefundOrderRepository refundOrderRepository, PayLockManager lockManager, PayLogSink logSink,
        PayEventListener eventListener, FeePolicy feePolicy) {
        this.registry = registry;
        this.payOrderRepository = payOrderRepository;
        this.refundOrderRepository = refundOrderRepository;
        this.lockManager = lockManager;
        this.logSink = logSink;
        this.eventListener = eventListener;
        this.feePolicy = feePolicy;
    }

    /**
     * 发起支付：查证并关闭上一笔未支付交易 → 计费 → 创建交易单 → 渠道下单 → 推进 PAYING。
     *
     * <p>宿主应仅在返回结果 {@code queryable=true} 时安排轮询兜底任务。</p>
     */
    public PayResult pay(PayCommand command) {
        requireText(command.getChannelCode(), "channelCode");
        requireText(command.getOrderNo(), "orderNo");
        requireText(command.getCurrency(), "currency");
        if (command.getPayAmount() == null || command.getPayAmount().signum() <= 0) {
            throw new PayChannelException(PayErrorCode.PAY_PARAM_INVALID, "payAmount 必须为正数");
        }
        String channel = command.getChannelCode();
        PayProvider payProvider = registry.require(channel, PayActionEnum.PAY);
        return lockManager.withLock(LOCK_PAY + command.getOrderNo(), () -> {
            if (isNotBlank(command.getLastOutTradeNo())) {
                ensureTradeClosed(channel, command.getOrderNo(), command.getLastOutTradeNo());
            }
            BigDecimal actualAmount =
                feePolicy.applyPayFee(channel, command.getPayAmount(), command.getCurrency());
            PayOrderSnapshot order = payOrderRepository.create(command, actualAmount);
            Object payload = invokeChannel(PayActionEnum.PAY, channel, command.getOrderNo(), order.getOutTradeNo(),
                command, () -> payProvider.pay(command, order));
            if (!payOrderRepository.transitionState(order.getOutTradeNo(), PayStateEnum.INIT, PayStateEnum.PAYING,
                null)) {
                log.warn("交易单推进 PAYING 失败（已被并发推进），outTradeNo: {}", order.getOutTradeNo());
            }
            return PayResult.builder()
                .orderNo(command.getOrderNo())
                .outTradeNo(order.getOutTradeNo())
                .payAmount(actualAmount)
                .currency(command.getCurrency())
                .channelPayload(payload)
                .queryable(registry.supports(channel, PayActionEnum.QUERY))
                .build();
        });
    }

    /**
     * 处理异步回调，返回应答渠道的报文体。
     *
     * <p>幂等：订单已处理过（终态）时直接返回应答报文，不重复推进、不重复发事件。</p>
     */
    public String handleCallback(CallbackCommand command) {
        requireText(command.getChannelCode(), "channelCode");
        requireText(command.getOrderNo(), "orderNo");
        requireText(command.getOutTradeNo(), "outTradeNo");
        String channel = command.getChannelCode();
        CallbackProvider provider = registry.require(channel, PayActionEnum.CALLBACK);
        return lockManager.withLock(resultLockKey(channel, command.getOrderNo()), () -> {
            log.info("支付回调处理开始, 渠道: {}, 订单号: {}, 交易号: {}", channel, command.getOrderNo(),
                command.getOutTradeNo());
            CallbackResult callback = invokeChannel(PayActionEnum.CALLBACK, channel, command.getOrderNo(),
                command.getOutTradeNo(), summarizeCallback(command), () -> provider.handleCallback(command));
            if (!callback.isPaid()) {
                log.warn("支付回调声明未支付, 渠道: {}, 订单号: {}, 交易号: {}", channel, command.getOrderNo(),
                    command.getOutTradeNo());
                return callback.getAckBody();
            }
            String thirdOutTradeNo = callback.getThirdOutTradeNo();
            if (callback.isReQueryRequired()) {
                QueryProvider queryProvider = registry.require(channel, PayActionEnum.QUERY);
                QueryCommand queryCommand = QueryCommand.builder().channelCode(channel)
                    .orderNo(command.getOrderNo()).outTradeNo(command.getOutTradeNo())
                    .thirdOutTradeNo(thirdOutTradeNo).build();
                QueryResult query = invokeChannel(PayActionEnum.QUERY, channel, command.getOrderNo(),
                    command.getOutTradeNo(), queryCommand, () -> queryProvider.query(queryCommand));
                if (!query.isPaid()) {
                    throw new PayChannelException(PayErrorCode.CALLBACK_QUERY_UNPAID,
                        "outTradeNo=" + command.getOutTradeNo());
                }
                if (thirdOutTradeNo == null) {
                    thirdOutTradeNo = query.getThirdOutTradeNo();
                }
            }
            PayOrderSnapshot order =
                payOrderRepository.find(channel, command.getOrderNo(), command.getOutTradeNo());
            if (order == null) {
                throw new PayChannelException(PayErrorCode.ORDER_DOES_NOT_EXIST,
                    "outTradeNo=" + command.getOutTradeNo());
            }
            boolean advanced = markPaySuccess(order, thirdOutTradeNo);
            log.info("支付回调处理结束, 渠道: {}, 订单号: {}, 交易号: {}, 本次推进: {}", channel,
                command.getOrderNo(), command.getOutTradeNo(), advanced);
            return callback.getAckBody();
        });
    }

    /**
     * 主动查询支付结果（轮询兜底任务调用）。
     *
     * @return true 表示已支付（含此前已处理过的幂等返回），false 表示仍未支付，宿主可继续轮询
     * @throws PayChannelException 渠道未实现 QUERY 时抛 PAYMENT_ACTION_UNSUPPORTED——
     *                             宿主不应把 queryable=false 的交易放进轮询队列
     */
    public boolean queryPayResult(QueryCommand command) {
        requireText(command.getChannelCode(), "channelCode");
        requireText(command.getOrderNo(), "orderNo");
        requireText(command.getOutTradeNo(), "outTradeNo");
        String channel = command.getChannelCode();
        QueryProvider provider = registry.require(channel, PayActionEnum.QUERY);
        return lockManager.withLock(resultLockKey(channel, command.getOrderNo()), () -> {
            PayOrderSnapshot order =
                payOrderRepository.find(channel, command.getOrderNo(), command.getOutTradeNo());
            if (order == null) {
                throw new PayChannelException(PayErrorCode.ORDER_DOES_NOT_EXIST,
                    "outTradeNo=" + command.getOutTradeNo());
            }
            if (!PayStateMachine.INSTANCE.canTransition(order.getState(), PayEventEnum.PAY_SUCCESS)) {
                // 终态（已成功/已关闭/已失败），轮询任务无须继续
                return true;
            }
            QueryResult query = invokeChannel(PayActionEnum.QUERY, channel, command.getOrderNo(),
                command.getOutTradeNo(), command, () -> provider.query(command));
            if (!query.isPaid()) {
                return false;
            }
            markPaySuccess(order, query.getThirdOutTradeNo());
            return true;
        });
    }

    /**
     * 退款。
     *
     * <p>失败语义（修正原模块缺陷）：渠道返回失败或抛异常时，退款单都会以
     * REFUNDING → FAIL 留下失败记录（原模块失败分支误用 INIT 为源状态，必抛非法流转且整体回滚无痕）。</p>
     *
     * @return true 退款成功；false 渠道拒绝（退款单已置 FAIL）
     */
    public boolean refund(RefundCommand command) {
        requireText(command.getChannelCode(), "channelCode");
        requireText(command.getOrderNo(), "orderNo");
        requireText(command.getOutTradeNo(), "outTradeNo");
        requireText(command.getRefundOrderNo(), "refundOrderNo");
        if (command.getRefundAmount() == null || command.getRefundAmount().signum() <= 0) {
            throw new PayChannelException(PayErrorCode.REFUND_AMOUNT_INVALID,
                "refundAmount=" + command.getRefundAmount());
        }
        String channel = command.getChannelCode();
        if (!command.isOutRefund()) {
            // 提前校验渠道能力，避免创建退款单后才发现不支持
            registry.require(channel, PayActionEnum.REFUND);
        }
        return lockManager.withLock(LOCK_REFUND + command.getOrderNo(), () -> doRefund(command, channel));
    }

    private boolean doRefund(RefundCommand command, String channel) {
        PayOrderSnapshot order = payOrderRepository.find(channel, command.getOrderNo(), command.getOutTradeNo());
        if (order == null) {
            throw new PayChannelException(PayErrorCode.ORDER_DOES_NOT_EXIST,
                "outTradeNo=" + command.getOutTradeNo());
        }
        if (order.getState() != PayStateEnum.SUCCESS) {
            throw new PayChannelException(PayErrorCode.ORDER_NOT_PAID, "state=" + order.getState());
        }
        BigDecimal refundable = order.getRefundableAmount();
        if (refundable == null || refundable.signum() <= 0) {
            throw new PayChannelException(PayErrorCode.ORDER_REFUND_FINISH,
                "outTradeNo=" + command.getOutTradeNo());
        }
        BigDecimal amount = command.getRefundAmount();
        if (refundable.compareTo(amount) < 0) {
            log.info("退款金额超出可退金额, 订单号: {}, 申请: {}, 可退: {}", command.getOrderNo(), amount, refundable);
            amount = refundable;
        }
        boolean allRefund = amount.compareTo(refundable) == 0;
        refundOrderRepository.create(command.getOutTradeNo(), command.getRefundOrderNo(), amount,
            order.getCurrency());
        // INIT -> REFUNDING（先落"退款中"，无论后续成败都有痕迹）
        RefundStateEnum refunding =
            RefundStateMachine.INSTANCE.transition(RefundStateEnum.INIT, RefundEventEnum.REFUND_REQUEST);
        refundOrderRepository.transitionState(command.getRefundOrderNo(), RefundStateEnum.INIT, refunding);

        RefundResult result;
        if (command.isOutRefund()) {
            // 外部已退款补录，不调渠道
            result = RefundResult.builder().success(true).build();
        } else {
            RefundProvider provider = registry.require(channel, PayActionEnum.REFUND);
            ChannelRefundCommand channelCommand = ChannelRefundCommand.builder()
                .channelCode(channel).orderNo(command.getOrderNo()).outTradeNo(command.getOutTradeNo())
                .refundOrderNo(command.getRefundOrderNo()).refundAmount(amount).currency(order.getCurrency())
                .thirdOutTradeNo(order.getThirdOutTradeNo()).allRefund(allRefund)
                .userId(command.getUserId()).extras(command.getExtras()).build();
            try {
                result = invokeChannel(PayActionEnum.REFUND, channel, command.getOrderNo(),
                    command.getOutTradeNo(), channelCommand, () -> provider.refund(channelCommand));
            } catch (RuntimeException e) {
                // 渠道异常也要留下 FAIL 痕迹再上抛（源状态用流转后的 REFUNDING，修正原模块 S1 缺陷）
                refundOrderRepository.transitionState(command.getRefundOrderNo(), refunding,
                    RefundStateMachine.INSTANCE.transition(refunding, RefundEventEnum.REFUND_FAIL));
                throw e;
            }
        }
        if (result != null && result.isSuccess()) {
            refundOrderRepository.transitionState(command.getRefundOrderNo(), refunding,
                RefundStateMachine.INSTANCE.transition(refunding, RefundEventEnum.REFUND_SUCCESS));
            payOrderRepository.applyRefund(command.getOutTradeNo(), amount, allRefund);
            eventListener.onRefundSuccess(channel, command.getOrderNo(), command.getRefundOrderNo(), amount);
            return true;
        }
        log.warn("渠道退款失败, 订单号: {}, 退款单号: {}, 原因: {}", command.getOrderNo(),
            command.getRefundOrderNo(), result == null ? null : result.getFailReason());
        refundOrderRepository.transitionState(command.getRefundOrderNo(), refunding,
            RefundStateMachine.INSTANCE.transition(refunding, RefundEventEnum.REFUND_FAIL));
        return false;
    }

    /**
     * 取消交易：先查证渠道侧未支付，再关闭渠道交易并推进本地 CLOSED。
     *
     * @throws PayChannelException 渠道侧已支付时抛 ORDER_HAS_PAID（宿主据此走补单流程，
     *                             替代原模块经 ThreadLocal 带外传值的方式）
     */
    public boolean cancel(String channelCode, String orderNo, String outTradeNo) {
        requireText(channelCode, "channelCode");
        requireText(orderNo, "orderNo");
        requireText(outTradeNo, "outTradeNo");
        return lockManager.withLock(LOCK_CANCEL + channelCode + ":" + orderNo, () -> {
            ensureTradeClosed(channelCode, orderNo, outTradeNo);
            return true;
        });
    }

    /**
     * 二段式扣款确认（修正原模块 S4：走状态机幂等，重复调用不重复扣款、不重复发事件）。
     *
     * @return true 扣款成功（含此前已成功的幂等返回）
     */
    public boolean executePayment(ExecutePaymentCommand command) {
        requireText(command.getChannelCode(), "channelCode");
        requireText(command.getOrderNo(), "orderNo");
        requireText(command.getOutTradeNo(), "outTradeNo");
        String channel = command.getChannelCode();
        ExecutePaymentProvider provider = registry.require(channel, PayActionEnum.EXECUTE_PAYMENT);
        return lockManager.withLock(resultLockKey(channel, command.getOrderNo()), () -> {
            PayOrderSnapshot order =
                payOrderRepository.find(channel, command.getOrderNo(), command.getOutTradeNo());
            if (order == null) {
                throw new PayChannelException(PayErrorCode.ORDER_DOES_NOT_EXIST,
                    "outTradeNo=" + command.getOutTradeNo());
            }
            if (!PayStateMachine.INSTANCE.canTransition(order.getState(), PayEventEnum.PAY_SUCCESS)) {
                // 终态幂等：已成功返回 true，已失败/关闭返回 false，都不再调渠道
                return order.getState() == PayStateEnum.SUCCESS;
            }
            ExecutePaymentResult result = invokeChannel(PayActionEnum.EXECUTE_PAYMENT, channel,
                command.getOrderNo(), command.getOutTradeNo(), command, () -> provider.executePayment(command));
            if (result != null && result.isSuccess()) {
                markPaySuccess(order, result.getThirdOutTradeNo());
                return true;
            }
            PayStateEnum failState =
                PayStateMachine.INSTANCE.transition(order.getState(), PayEventEnum.PAY_FAIL);
            payOrderRepository.transitionState(order.getOutTradeNo(), order.getState(), failState, null);
            return false;
        });
    }

    /**
     * 推进支付成功并发布事件。锁 + canTransition + CAS 三重防护，事件至多发布一次。
     *
     * @return 本次是否完成推进（false = 已被处理过，属正常幂等路径）
     */
    private boolean markPaySuccess(PayOrderSnapshot order, String thirdOutTradeNo) {
        if (!PayStateMachine.INSTANCE.canTransition(order.getState(), PayEventEnum.PAY_SUCCESS)) {
            log.info("支付结果已处理过, 订单号: {}, 交易号: {}, 当前状态: {}", order.getOrderNo(),
                order.getOutTradeNo(), order.getState());
            return false;
        }
        PayStateEnum target = PayStateMachine.INSTANCE.transition(order.getState(), PayEventEnum.PAY_SUCCESS);
        boolean advanced = payOrderRepository.transitionState(order.getOutTradeNo(), order.getState(), target,
            thirdOutTradeNo);
        if (!advanced) {
            log.info("支付结果已被并发处理, 订单号: {}, 交易号: {}", order.getOrderNo(), order.getOutTradeNo());
            return false;
        }
        eventListener.onPaySuccess(order.getChannelCode(), order.getOrderNo(), order.getOutTradeNo());
        return true;
    }

    /**
     * 查证并关闭一笔交易：渠道侧已支付则抛 ORDER_HAS_PAID；本地可关闭时要求渠道 CANCEL 成功后推进 CLOSED。
     */
    private void ensureTradeClosed(String channelCode, String orderNo, String outTradeNo) {
        if (registry.supports(channelCode, PayActionEnum.QUERY)) {
            QueryProvider queryProvider = registry.require(channelCode, PayActionEnum.QUERY);
            QueryCommand queryCommand = QueryCommand.builder().channelCode(channelCode).orderNo(orderNo)
                .outTradeNo(outTradeNo).build();
            QueryResult query = invokeChannel(PayActionEnum.QUERY, channelCode, orderNo, outTradeNo, queryCommand,
                () -> queryProvider.query(queryCommand));
            if (query.isPaid()) {
                throw new PayChannelException(PayErrorCode.ORDER_HAS_PAID, "outTradeNo=" + outTradeNo);
            }
        }
        PayOrderSnapshot order = payOrderRepository.find(channelCode, orderNo, outTradeNo);
        if (order == null || !PayStateMachine.INSTANCE.canTransition(order.getState(), PayEventEnum.CLOSE)) {
            return;
        }
        CancelProvider cancelProvider = registry.require(channelCode, PayActionEnum.CANCEL);
        CancelCommand cancelCommand = CancelCommand.builder().channelCode(channelCode).orderNo(orderNo)
            .outTradeNo(outTradeNo).thirdOutTradeNo(order.getThirdOutTradeNo()).build();
        boolean cancelled = invokeChannel(PayActionEnum.CANCEL, channelCode, orderNo, outTradeNo, cancelCommand,
            () -> cancelProvider.cancel(cancelCommand));
        if (!cancelled) {
            throw new PayChannelException(PayErrorCode.ORDER_PAYING_WAITE_REFUND, "outTradeNo=" + outTradeNo);
        }
        PayStateEnum closed = PayStateMachine.INSTANCE.transition(order.getState(), PayEventEnum.CLOSE);
        payOrderRepository.transitionState(outTradeNo, order.getState(), closed, null);
    }

    /**
     * 渠道调用统一入口：计时、请求/响应日志采集（同步组装，无 ThreadLocal）、异常包装。
     */
    private <T> T invokeChannel(PayActionEnum action, String channelCode, String orderNo, String outTradeNo,
        Object requestForLog, Supplier<T> invocation) {
        long start = System.currentTimeMillis();
        T response = null;
        Throwable error = null;
        try {
            response = invocation.get();
            return response;
        } catch (PayChannelException e) {
            error = e;
            throw e;
        } catch (RuntimeException e) {
            error = e;
            throw new PayChannelException(PayErrorCode.CHANNEL_INVOKE_ERROR,
                "channel=" + channelCode + ", action=" + action + ", cause=" + e.getMessage(), e);
        } finally {
            recordLog(action, channelCode, orderNo, outTradeNo, requestForLog, response, error,
                System.currentTimeMillis() - start);
        }
    }

    private void recordLog(PayActionEnum action, String channelCode, String orderNo, String outTradeNo,
        Object request, Object response, Throwable error, long costMillis) {
        try {
            logSink.record(PayLogRecord.builder()
                .channelCode(channelCode).action(action).orderNo(orderNo).outTradeNo(outTradeNo)
                .requestJson(Jsons.toJson(request)).responseJson(Jsons.toJson(response))
                .errorMessage(error == null ? null : error.toString())
                .costMillis(costMillis).build());
        } catch (Exception logEx) {
            log.error("支付日志记录异常，不影响主流程", logEx);
        }
    }

    private String resultLockKey(String channelCode, String orderNo) {
        return LOCK_RESULT + channelCode + ":" + orderNo;
    }

    /** 回调命令的日志摘要：不含 headers（可能有敏感凭据），保留原始 body 供对账 */
    private Object summarizeCallback(CallbackCommand command) {
        return command.getBody() != null ? command.getBody() : command.getParsedRequest();
    }

    private static void requireText(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new PayChannelException(PayErrorCode.PAY_PARAM_INVALID, field + " 不能为空");
        }
    }

    private static boolean isNotBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
