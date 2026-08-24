package com.wallet.channel.core;

import com.wallet.channel.action.CallbackAction;
import com.wallet.channel.action.CancelAction;
import com.wallet.channel.action.ConfirmAction;
import com.wallet.channel.action.PayAction;
import com.wallet.channel.action.QueryAction;
import com.wallet.channel.action.RefundAction;
import com.wallet.channel.enums.ActionType;
import com.wallet.channel.enums.PayError;
import com.wallet.channel.enums.PayEvent;
import com.wallet.channel.enums.PayState;
import com.wallet.channel.enums.RefundEvent;
import com.wallet.channel.enums.RefundState;
import com.wallet.channel.error.ChannelException;
import com.wallet.channel.model.CallLog;
import com.wallet.channel.model.CallbackRequest;
import com.wallet.channel.model.CallbackResult;
import com.wallet.channel.model.CancelRequest;
import com.wallet.channel.model.ChannelRefundRequest;
import com.wallet.channel.model.ConfirmRequest;
import com.wallet.channel.model.ConfirmResult;
import com.wallet.channel.model.PayRequest;
import com.wallet.channel.model.PayResult;
import com.wallet.channel.model.QueryRequest;
import com.wallet.channel.model.QueryResult;
import com.wallet.channel.model.RefundRequest;
import com.wallet.channel.model.RefundResult;
import com.wallet.channel.model.TradeInfo;
import com.wallet.channel.spi.CallLogWriter;
import com.wallet.channel.spi.FeeRule;
import com.wallet.channel.spi.PayListener;
import com.wallet.channel.spi.RefundStore;
import com.wallet.channel.spi.TradeStore;
import com.wallet.channel.state.PayStateMachine;
import com.wallet.channel.state.RefundStateMachine;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Supplier;

/**
 * 渠道支付编排器。
 *
 * <p><b>锁约定（重要）：内核不加锁。</b>调用方必须在调用 pay / callback / query /
 * refund / cancel / confirm 前持有该支付单的分布式锁（钱包工程里是
 * {@code wallet:lock:order:{orderNo}} 一把锁管所有状态变更）。</p>
 *
 * <p>并发与幂等模型：</p>
 * <ul>
 *   <li>所有状态推进走 TradeStore/RefundStore 的条件更新（CAS），
 *       外层锁 + 状态机 canTransition + CAS 三重保证支付成功事件对同一交易至多发布一次；</li>
 *   <li>内核不开数据库事务，渠道 HTTP 调用不在任何事务内，每次落库都是独立的短写入。</li>
 * </ul>
 */
@Slf4j
public final class PayFlow {

    private final ChannelTable table;
    private final TradeStore tradeStore;
    private final RefundStore refundStore;
    private final CallLogWriter logWriter;
    private final PayListener listener;
    private final FeeRule feeRule;

    /** 请通过 {@code ChannelKit.builder()} 组装，不要直接实例化（Kit 负责注册表完整性校验） */
    public PayFlow(ChannelTable table, TradeStore tradeStore, RefundStore refundStore, CallLogWriter logWriter,
        PayListener listener, FeeRule feeRule) {
        this.table = table;
        this.tradeStore = tradeStore;
        this.refundStore = refundStore;
        this.logWriter = logWriter;
        this.listener = listener;
        this.feeRule = feeRule;
    }

    /**
     * 发起支付：查证并关闭上一笔未支付交易 → 计费 → 创建交易单 → 渠道下单 → 推进 PAYING。
     *
     * <p>调用方应仅在返回结果 {@code queryable=true} 时安排轮询兜底任务。</p>
     */
    public PayResult pay(PayRequest request) {
        requireText(request.channelCode(), "channelCode");
        requireText(request.orderNo(), "orderNo");
        requireText(request.currency(), "currency");
        if (request.amount() <= 0) {
            throw new ChannelException(PayError.PAY_PARAM_INVALID, "amount 必须为正数");
        }
        String channel = request.channelCode();
        PayAction payAction = table.require(channel, ActionType.PAY);
        if (isNotBlank(request.lastOutTradeNo())) {
            closeOldTrade(channel, request.orderNo(), request.lastOutTradeNo());
        }
        long realAmount = feeRule.applyFee(channel, request.amount(), request.currency());
        TradeInfo trade = tradeStore.create(request, realAmount);
        Object payload = callChannel(ActionType.PAY, channel, request.orderNo(), trade.outTradeNo(),
            request, () -> payAction.pay(request, trade));
        if (!tradeStore.changeState(trade.outTradeNo(), PayState.INIT, PayState.PAYING, null)) {
            log.warn("交易单推进 PAYING 失败（已被并发推进），outTradeNo: {}", trade.outTradeNo());
        }
        return PayResult.builder()
            .orderNo(request.orderNo())
            .outTradeNo(trade.outTradeNo())
            .amount(realAmount)
            .currency(request.currency())
            .channelPayload(payload)
            .queryable(table.supports(channel, ActionType.QUERY))
            .build();
    }

    /**
     * 处理异步回调，返回应答渠道的报文体。
     *
     * <p>幂等：订单已处理过（终态）时直接返回应答报文，不重复推进、不重复发事件。</p>
     */
    public String callback(CallbackRequest request) {
        requireText(request.channelCode(), "channelCode");
        requireText(request.orderNo(), "orderNo");
        requireText(request.outTradeNo(), "outTradeNo");
        String channel = request.channelCode();
        CallbackAction action = table.require(channel, ActionType.CALLBACK);
        log.info("支付回调处理开始, 渠道: {}, 订单号: {}, 交易号: {}", channel, request.orderNo(),
            request.outTradeNo());
        CallbackResult result = callChannel(ActionType.CALLBACK, channel, request.orderNo(),
            request.outTradeNo(), request.body(), () -> action.onCallback(request));
        if (!result.paid()) {
            log.warn("支付回调声明未支付, 渠道: {}, 订单号: {}, 交易号: {}", channel, request.orderNo(),
                request.outTradeNo());
            return result.ackBody();
        }
        String thirdOutTradeNo = result.thirdOutTradeNo();
        if (result.reQueryRequired()) {
            // 不信任回调报文的渠道：以主动查询结果为准
            QueryAction queryAction = table.require(channel, ActionType.QUERY);
            QueryRequest queryRequest =
                new QueryRequest(channel, request.orderNo(), request.outTradeNo(), thirdOutTradeNo);
            QueryResult query = callChannel(ActionType.QUERY, channel, request.orderNo(),
                request.outTradeNo(), queryRequest, () -> queryAction.query(queryRequest));
            if (!query.paid()) {
                throw new ChannelException(PayError.CALLBACK_QUERY_UNPAID,
                    "outTradeNo=" + request.outTradeNo());
            }
            if (thirdOutTradeNo == null) {
                thirdOutTradeNo = query.thirdOutTradeNo();
            }
        }
        TradeInfo trade = findTrade(channel, request.orderNo(), request.outTradeNo());
        boolean advanced = markSuccess(trade, thirdOutTradeNo);
        log.info("支付回调处理结束, 渠道: {}, 订单号: {}, 交易号: {}, 本次推进: {}", channel,
            request.orderNo(), request.outTradeNo(), advanced);
        return result.ackBody();
    }

    /**
     * 主动查询支付结果（轮询兜底任务调用）。
     *
     * @return true 表示已支付或已终态（轮询可停止），false 表示仍未支付可继续轮询
     * @throws ChannelException 渠道未实现 QUERY 时抛 PAYMENT_ACTION_UNSUPPORTED——
     *                          调用方不应把 queryable=false 的交易放进轮询队列
     */
    public boolean query(QueryRequest request) {
        requireText(request.channelCode(), "channelCode");
        requireText(request.orderNo(), "orderNo");
        requireText(request.outTradeNo(), "outTradeNo");
        String channel = request.channelCode();
        QueryAction action = table.require(channel, ActionType.QUERY);
        TradeInfo trade = findTrade(channel, request.orderNo(), request.outTradeNo());
        if (!PayStateMachine.INSTANCE.canTransition(trade.state(), PayEvent.PAY_SUCCESS)) {
            // 终态（已成功/已关闭/已失败），轮询任务无须继续
            return true;
        }
        QueryResult query = callChannel(ActionType.QUERY, channel, request.orderNo(), request.outTradeNo(),
            request, () -> action.query(request));
        if (!query.paid()) {
            return false;
        }
        markSuccess(trade, query.thirdOutTradeNo());
        return true;
    }

    /**
     * 退款。
     *
     * <p>失败语义：渠道返回失败或抛异常时，退款单都会以 REFUNDING → FAIL 留下失败记录。</p>
     *
     * @return true 退款成功；false 渠道拒绝（退款单已置 FAIL）
     */
    public boolean refund(RefundRequest request) {
        requireText(request.channelCode(), "channelCode");
        requireText(request.orderNo(), "orderNo");
        requireText(request.outTradeNo(), "outTradeNo");
        requireText(request.refundOrderNo(), "refundOrderNo");
        if (request.amount() <= 0) {
            throw new ChannelException(PayError.REFUND_AMOUNT_INVALID, "amount=" + request.amount());
        }
        String channel = request.channelCode();
        if (!request.outRefund()) {
            // 提前校验渠道能力，避免创建退款单后才发现不支持
            table.require(channel, ActionType.REFUND);
        }

        TradeInfo trade = findTrade(channel, request.orderNo(), request.outTradeNo());
        if (trade.state() != PayState.SUCCESS) {
            throw new ChannelException(PayError.ORDER_NOT_PAID, "state=" + trade.state());
        }
        long refundable = trade.refundableAmount();
        if (refundable <= 0) {
            throw new ChannelException(PayError.ORDER_REFUND_FINISH, "outTradeNo=" + request.outTradeNo());
        }
        long amount = request.amount();
        if (amount > refundable) {
            log.info("退款金额超出可退金额, 订单号: {}, 申请: {}, 可退: {}", request.orderNo(), amount,
                refundable);
            amount = refundable;
        }
        boolean allRefund = amount == refundable;
        refundStore.create(request.outTradeNo(), request.refundOrderNo(), amount, trade.currency());
        // INIT -> REFUNDING（先落"退款中"，无论后续成败都有痕迹）
        RefundState refunding =
            RefundStateMachine.INSTANCE.transition(RefundState.INIT, RefundEvent.REFUND_REQUEST);
        refundStore.changeState(request.refundOrderNo(), RefundState.INIT, refunding);

        RefundResult result;
        if (request.outRefund()) {
            // 外部已退款补录，不调渠道
            result = RefundResult.ok(null);
        } else {
            RefundAction action = table.require(channel, ActionType.REFUND);
            ChannelRefundRequest channelRequest = ChannelRefundRequest.builder()
                .channelCode(channel).orderNo(request.orderNo()).outTradeNo(request.outTradeNo())
                .refundOrderNo(request.refundOrderNo()).amount(amount).currency(trade.currency())
                .thirdOutTradeNo(trade.thirdOutTradeNo()).allRefund(allRefund)
                .userId(request.userId()).extras(request.extras()).build();
            try {
                result = callChannel(ActionType.REFUND, channel, request.orderNo(), request.outTradeNo(),
                    channelRequest, () -> action.refund(channelRequest));
            } catch (RuntimeException e) {
                // 渠道异常也要留下 FAIL 痕迹再上抛
                refundStore.changeState(request.refundOrderNo(), refunding,
                    RefundStateMachine.INSTANCE.transition(refunding, RefundEvent.REFUND_FAIL));
                throw e;
            }
        }
        if (result != null && result.success()) {
            refundStore.changeState(request.refundOrderNo(), refunding,
                RefundStateMachine.INSTANCE.transition(refunding, RefundEvent.REFUND_SUCCESS));
            tradeStore.applyRefund(request.outTradeNo(), amount, allRefund);
            listener.onRefundSuccess(channel, request.orderNo(), request.refundOrderNo(), amount);
            return true;
        }
        log.warn("渠道退款失败, 订单号: {}, 退款单号: {}, 原因: {}", request.orderNo(),
            request.refundOrderNo(), result == null ? null : result.failReason());
        refundStore.changeState(request.refundOrderNo(), refunding,
            RefundStateMachine.INSTANCE.transition(refunding, RefundEvent.REFUND_FAIL));
        return false;
    }

    /**
     * 取消交易：先查证渠道侧未支付，再关闭渠道交易并推进本地 CLOSED。
     *
     * @throws ChannelException 渠道侧已支付时抛 ORDER_HAS_PAID（调用方据此走补单流程）
     */
    public boolean cancel(String channelCode, String orderNo, String outTradeNo) {
        requireText(channelCode, "channelCode");
        requireText(orderNo, "orderNo");
        requireText(outTradeNo, "outTradeNo");
        closeOldTrade(channelCode, orderNo, outTradeNo);
        return true;
    }

    /**
     * 二段式扣款确认（走状态机幂等：重复调用不重复扣款、不重复发事件）。
     *
     * @return true 扣款成功（含此前已成功的幂等返回）
     */
    public boolean confirm(ConfirmRequest request) {
        requireText(request.channelCode(), "channelCode");
        requireText(request.orderNo(), "orderNo");
        requireText(request.outTradeNo(), "outTradeNo");
        String channel = request.channelCode();
        ConfirmAction action = table.require(channel, ActionType.CONFIRM);
        TradeInfo trade = findTrade(channel, request.orderNo(), request.outTradeNo());
        if (!PayStateMachine.INSTANCE.canTransition(trade.state(), PayEvent.PAY_SUCCESS)) {
            // 终态幂等：已成功返回 true，已失败/关闭返回 false，都不再调渠道
            return trade.state() == PayState.SUCCESS;
        }
        ConfirmResult result = callChannel(ActionType.CONFIRM, channel, request.orderNo(),
            request.outTradeNo(), request, () -> action.confirm(request));
        if (result != null && result.success()) {
            markSuccess(trade, result.thirdOutTradeNo());
            return true;
        }
        PayState failState = PayStateMachine.INSTANCE.transition(trade.state(), PayEvent.PAY_FAIL);
        tradeStore.changeState(trade.outTradeNo(), trade.state(), failState, null);
        return false;
    }

    /**
     * 推进支付成功并发布事件。外层锁 + canTransition + CAS 三重防护，事件至多发布一次。
     *
     * @return 本次是否完成推进（false = 已被处理过，属正常幂等路径）
     */
    private boolean markSuccess(TradeInfo trade, String thirdOutTradeNo) {
        if (!PayStateMachine.INSTANCE.canTransition(trade.state(), PayEvent.PAY_SUCCESS)) {
            log.info("支付结果已处理过, 订单号: {}, 交易号: {}, 当前状态: {}", trade.orderNo(),
                trade.outTradeNo(), trade.state());
            return false;
        }
        PayState target = PayStateMachine.INSTANCE.transition(trade.state(), PayEvent.PAY_SUCCESS);
        boolean advanced = tradeStore.changeState(trade.outTradeNo(), trade.state(), target, thirdOutTradeNo);
        if (!advanced) {
            log.info("支付结果已被并发处理, 订单号: {}, 交易号: {}", trade.orderNo(), trade.outTradeNo());
            return false;
        }
        listener.onPaySuccess(trade.channelCode(), trade.orderNo(), trade.outTradeNo());
        return true;
    }

    /**
     * 查证并关闭一笔交易：渠道侧已支付则抛 ORDER_HAS_PAID；本地可关闭时要求渠道 CANCEL 成功后推进 CLOSED。
     */
    private void closeOldTrade(String channelCode, String orderNo, String outTradeNo) {
        if (table.supports(channelCode, ActionType.QUERY)) {
            QueryAction queryAction = table.require(channelCode, ActionType.QUERY);
            QueryRequest queryRequest = new QueryRequest(channelCode, orderNo, outTradeNo);
            QueryResult query = callChannel(ActionType.QUERY, channelCode, orderNo, outTradeNo, queryRequest,
                () -> queryAction.query(queryRequest));
            if (query.paid()) {
                throw new ChannelException(PayError.ORDER_HAS_PAID, "outTradeNo=" + outTradeNo);
            }
        }
        TradeInfo trade = tradeStore.find(channelCode, orderNo, outTradeNo);
        if (trade == null || !PayStateMachine.INSTANCE.canTransition(trade.state(), PayEvent.CLOSE)) {
            return;
        }
        CancelAction cancelAction = table.require(channelCode, ActionType.CANCEL);
        CancelRequest cancelRequest =
            new CancelRequest(channelCode, orderNo, outTradeNo, trade.thirdOutTradeNo());
        boolean cancelled = callChannel(ActionType.CANCEL, channelCode, orderNo, outTradeNo, cancelRequest,
            () -> cancelAction.cancel(cancelRequest));
        if (!cancelled) {
            throw new ChannelException(PayError.ORDER_PAYING_WAITE_REFUND, "outTradeNo=" + outTradeNo);
        }
        PayState closed = PayStateMachine.INSTANCE.transition(trade.state(), PayEvent.CLOSE);
        tradeStore.changeState(outTradeNo, trade.state(), closed, null);
    }

    private TradeInfo findTrade(String channelCode, String orderNo, String outTradeNo) {
        TradeInfo trade = tradeStore.find(channelCode, orderNo, outTradeNo);
        if (trade == null) {
            throw new ChannelException(PayError.ORDER_DOES_NOT_EXIST, "outTradeNo=" + outTradeNo);
        }
        return trade;
    }

    /**
     * 渠道调用统一入口：计时、请求/响应日志采集、异常包装。
     */
    private <T> T callChannel(ActionType action, String channelCode, String orderNo, String outTradeNo,
        Object requestForLog, Supplier<T> invocation) {
        long start = System.currentTimeMillis();
        T response = null;
        Throwable error = null;
        try {
            response = invocation.get();
            return response;
        } catch (ChannelException e) {
            error = e;
            throw e;
        } catch (RuntimeException e) {
            error = e;
            throw new ChannelException(PayError.CHANNEL_INVOKE_ERROR,
                "channel=" + channelCode + ", action=" + action + ", cause=" + e.getMessage(), e);
        } finally {
            writeLog(action, channelCode, orderNo, outTradeNo, requestForLog, response, error,
                System.currentTimeMillis() - start);
        }
    }

    private void writeLog(ActionType action, String channelCode, String orderNo, String outTradeNo,
        Object request, Object response, Throwable error, long costMs) {
        try {
            logWriter.write(new CallLog(channelCode, action, orderNo, outTradeNo, request, response,
                error == null ? null : error.toString(), costMs));
        } catch (Exception logEx) {
            log.error("渠道日志记录异常，不影响主流程", logEx);
        }
    }

    private static void requireText(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new ChannelException(PayError.PAY_PARAM_INVALID, field + " 不能为空");
        }
    }

    private static boolean isNotBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
