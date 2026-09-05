package com.wallet.pay.serviceImpl.pay;
import com.wallet.contract.pay.AssetPartService;

import com.wallet.contract.pay.PayService;
import com.wallet.contract.pay.OrderFinisher;
import com.wallet.contract.channel.ChannelService;
import com.wallet.common.enums.ActionType;
import com.wallet.common.error.ErrorCode;
import com.baomidou.lock.annotation.Lock4j;
import com.wallet.contract.channel.error.ChannelException;
import com.wallet.contract.channel.model.CallbackRequest;
import com.wallet.contract.channel.model.PayRequest;
import com.wallet.contract.channel.model.PayResult;
import com.wallet.contract.channel.model.QueryRequest;
import com.wallet.common.error.CommonException;
import com.wallet.common.util.IdMaker;
import com.wallet.pay.config.PayProperties;
import com.wallet.pay.mock.MockNotifyService;
import com.wallet.pay.entity.PayOrder;
import com.wallet.pay.entity.PayPart;
import com.wallet.common.enums.PayType;
import com.wallet.common.error.ErrorCode;
import com.wallet.pay.event.OrderClosedEvent;
import com.wallet.pay.mapper.PayOrderMapper;
import com.wallet.pay.mapper.PayPartMapper;
import com.wallet.contract.pay.model.CreateOrderCmd;
import com.wallet.contract.pay.model.CreateOrderResult;
import com.wallet.contract.pay.model.OrderDetail;
import com.wallet.contract.pay.model.PartItem;
import com.wallet.contract.pay.model.PayOrderView;
import com.wallet.contract.pay.model.PayPartView;
import com.wallet.contract.pay.model.SubmitResult;
import com.wallet.common.enums.OrderEvent;
import com.wallet.common.enums.OrderState;
import com.wallet.common.enums.PartEvent;
import com.wallet.common.enums.PartState;
import com.wallet.pay.state.OrderStateMachine;
import com.wallet.pay.state.PartStateMachine;
import com.wallet.pay.validate.PayScene;
import com.wallet.pay.validate.PayValidationContext;
import com.wallet.pay.validate.PayValidatorChain;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 支付编排服务。
 *
 * <p><b>同一把锁</b>：提交/回调/查询/取消/退款/关单入口全部标注
 * {@code @Lock4j(name = "order", keys = "#orderNo")}（实际 key 为
 * {@code wallet:lock:order#orderNo}），同一支付单的所有状态变更串行执行。
 * 锁语义：等锁 3 秒快速失败、Redisson 看门狗自动续期、同线程可重入。</p>
 *
 * <p>时序：创建（校验+落单）→ 提交（扣资产段在一个本地事务内，三方向发起在事务外）
 * → 回调/查询确认 → 全部成功推主单 SUCCESS；渠道下单失败或超时则补偿回滚资产段。</p>
 */
@Slf4j
@Service
@AllArgsConstructor
public class PayServiceImpl implements PayService {

    private final PayOrderMapper payOrderMapper;
    private final PayPartMapper payPartMapper;
    private final ChannelService channelService;
    private final MockNotifyService mockNotifyService;
    private final PayProperties config;
    private final ApplicationEventPublisher events;
    private final AssetPartService assetPartService;

    private final OrderFinisher orderFinisher;
    private final PayValidatorChain validatorChain;


    /**
     * 创建支付单：校验分段合法性，落主单 + 全部分段（INIT）。不持锁——
     * 建单幂等靠 (app_id, biz_order_no) 唯一索引：同接入方同业务单号重复创建返回既有支付单
     * （金额不一致视为调用方错误直接拒绝）。
     */
    @Transactional

    @Override
    public CreateOrderResult create(String appId, Long userId, CreateOrderCmd cmd) {
        PayOrder exist = payOrderMapper.findByAppAndBizOrderNo(appId, cmd.bizOrderNo());
        if (exist != null) {
            return existingOrder(exist, cmd);
        }
        // 责任链校验：金额勾稽、分段条件必填、券规则引擎抵扣额
        validatorChain.validate(PayValidationContext.forCreate(appId, userId, cmd));

        String orderNo = IdMaker.next("P");
        LocalDateTime now = LocalDateTime.now();
        PayOrder order = new PayOrder();
        order.setOrderNo(orderNo);
        order.setAppId(appId);
        order.setBizOrderNo(cmd.bizOrderNo());
        order.setUserId(userId);
        order.setTotalAmount(cmd.totalAmount());
        order.setCurrency(cmd.currency());
        order.setState(OrderState.INIT);
        order.setExpireTime(now.plusMinutes(config.getExpireMinutes()));
        order.setRefundableAmount(0L);
        order.setRefundedAmount(0L);
        try {
            payOrderMapper.insert(order);
        } catch (DuplicateKeyException e) {
            // 并发重复建单：唯一索引兜底，返回先到的那单（分段尚未插入，直接返回安全）
            return existingOrder(payOrderMapper.findByAppAndBizOrderNo(appId, cmd.bizOrderNo()), cmd);
        }

        for (PartItem item : cmd.parts()) {
            PayPart part = new PayPart();
            part.setPartNo(IdMaker.next("T"));
            part.setOrderNo(orderNo);
            part.setUserId(userId);
            part.setPayType(item.payType());
            part.setAmount(item.amount());
            part.setPointCount(item.pointCount());
            part.setUserCouponId(item.userCouponId());
            part.setChannelCode(item.channelCode());
            part.setState(PartState.INIT);
            part.setRefundedAmount(0L);
            payPartMapper.insert(part);
        }
        return new CreateOrderResult(orderNo, order.getExpireTime());
    }

    /** 建单幂等命中：金额一致返回既有单，不一致视为调用方用错单号 */
    private CreateOrderResult existingOrder(PayOrder exist, CreateOrderCmd cmd) {
        if (exist.getTotalAmount() != cmd.totalAmount()) {
            throw new CommonException(ErrorCode.AMOUNT_NOT_MATCH,
                "业务单号已存在且金额不一致, bizOrderNo=" + cmd.bizOrderNo()
                    + ", 已有=" + exist.getTotalAmount() + ", 本次=" + cmd.totalAmount());
        }
        log.info("建单幂等命中, appId={}, bizOrderNo={}, orderNo={}",
            exist.getAppId(), cmd.bizOrderNo(), exist.getOrderNo());
        return new CreateOrderResult(exist.getOrderNo(), exist.getExpireTime());
    }

    /** 提交支付：持单锁，扣资产段（事务）+ 发起三方（事务外）。 */
    @Lock4j(name = "order", keys = "#orderNo")

    @Override
    public SubmitResult submit(Long userId, String orderNo, String ticket) {
        return doSubmit(userId, orderNo, ticket);
    }

    private SubmitResult doSubmit(Long userId, String orderNo, String ticket) {
        PayOrder order = payOrderMapper.findByOrderNo(orderNo);
        List<PayPart> parts = order == null ? List.of() : payPartMapper.findByOrderNo(orderNo);
        // 责任链校验：归属 → 终态拦截 → 票据校验并消费（INIT 首次提交且含余额段时）
        validatorChain.validate(PayValidationContext.forSubmit(userId, orderNo, order, parts, ticket));

        OrderState state = order.getState();
        if (state == OrderState.SUCCESS) {
            return new SubmitResult("SUCCESS", null, "订单已支付");
        }

        if (state == OrderState.INIT) {
            // 主单 INIT→PAYING（CAS，防并发重复提交）
            if (payOrderMapper.changeState(orderNo, OrderState.INIT, OrderState.PAYING) == 0) {
                log.info("提交支付被并发处理, orderNo={}", orderNo);
                return submitRetry(order, parts);
            }
            // 扣资产段：一个本地事务，任一失败整体回滚
            try {
                assetPartService.deductAssetParts(userId, parts.stream().map(this::toPartView).toList(), orderNo);
            } catch (RuntimeException e) {
                log.warn("资产扣减失败, orderNo={}, err={}", orderNo, e.getMessage());
                payOrderMapper.markFailed(orderNo, OrderState.PAYING, OrderState.FAIL,
                    "资产扣减失败: " + e.getMessage());
                markPartsFailed(parts);
                throw e;
            }
            PayPart channelPart = channelPartOf(parts);
            if (channelPart == null) {
                // 纯资产支付：当场完成（结单与事件发布统一走 OrderFinisher）
                orderFinisher.finishIfAllSuccess(orderNo);
                return new SubmitResult("SUCCESS", null, "支付成功");
            }
            return payChannel(userId, order, channelPart);
        }
        return submitRetry(order, parts);
    }

    /** 主单已是 PAYING（重复提交）：返回已有渠道支付参数或当前状态 */
    private SubmitResult submitRetry(PayOrder order, List<PayPart> parts) {
        if (order.getState() == OrderState.SUCCESS) {
            return new SubmitResult("SUCCESS", null, "订单已支付");
        }
        PayPart channelPart = channelPartOf(parts);
        if (channelPart != null && channelPart.getState() == PartState.PAYING) {
            return new SubmitResult("PAYING", channelPart.getChannelPayload(), "支付已发起，等待结果");
        }
        if (order.getState() == OrderState.FAIL) {
            throw new CommonException(ErrorCode.ORDER_STATE_INVALID, "订单支付失败");
        }
        return new SubmitResult(order.getState().name(), null, "当前状态 " + order.getState());
    }

    /** 发起三方支付（事务外调用内核），失败补偿回滚资产段 */
    private SubmitResult payChannel(Long userId, PayOrder order, PayPart channelPart) {
        PayRequest request = PayRequest.builder()
            .channelCode(channelPart.getChannelCode())
            .orderNo(order.getOrderNo())
            .orderType("WALLET")
            .amount(channelPart.getAmount())
            .currency(order.getCurrency())
            .userId(userId)
            .build();
        try {
            PayResult result = channelService.pay(request);
            payPartMapper.updatePayload(channelPart.getPartNo(), String.valueOf(result.channelPayload()));
            // 仅 mock 渠道需要宿主安排自动回调（真实渠道自行推送）
            mockNotifyService.scheduleAutoNotify(channelPart.getChannelCode(), order.getOrderNo(),
                channelPart.getPartNo());
            log.info("渠道支付已发起, orderNo={}, partNo={}, queryable={}", order.getOrderNo(),
                channelPart.getPartNo(), result.queryable());
            return new SubmitResult("PAYING", result.channelPayload(), "支付已发起，等待结果");
        } catch (RuntimeException e) {
            log.warn("渠道下单失败，补偿回滚资产段, orderNo={}, err={}", order.getOrderNo(), e.getMessage());
            assetPartService.rollbackAssetParts(order.getOrderNo(), order.getUserId());
            payOrderMapper.markFailed(order.getOrderNo(), OrderState.PAYING, OrderState.FAIL, "渠道下单失败: " + e.getMessage());
            throw new CommonException(ErrorCode.CHANNEL_PAY_FAILED, e.getMessage());
        }
    }

    /** 处理渠道异步回调（持单锁，内核不重复加锁） */
    @Lock4j(name = "order", keys = "#orderNo")

    @Override
    public String handleCallback(String channelCode, String orderNo, String partNo, String body,
        Map<String, String> headers, String httpMethod, String requestUri) {
        CallbackRequest request = CallbackRequest.builder()
            .channelCode(channelCode)
            .orderNo(orderNo)
            .outTradeNo(partNo)
            .httpMethod(httpMethod)
            .requestUri(requestUri)
            .headers(headers)
            .body(body)
            .build();
        return channelService.callback(request);
    }

    /** 主动向渠道查证（持单锁） */
    @Lock4j(name = "order", keys = "#orderNo")

    @Override
    public boolean query(Long userId, String orderNo) {
        return doQuery(userId, orderNo);
    }

    private boolean doQuery(Long userId, String orderNo) {
        PayOrder order = payOrderMapper.findByOrderNo(orderNo);
        validatorChain.validate(PayValidationContext.forOrder(PayScene.QUERY, userId, orderNo, order));
        if (order.getState() == OrderState.SUCCESS) {
            return true;
        }
        PayPart channelPart = channelPartOf(payPartMapper.findByOrderNo(orderNo));
        if (channelPart == null) {
            return true; // 无三方段
        }
        if (channelPart.getState() != PartState.INIT && channelPart.getState() != PartState.PAYING) {
            return true; // 分段已终态
        }
        QueryRequest request = new QueryRequest(channelPart.getChannelCode(), orderNo,
            channelPart.getPartNo(), channelPart.getThirdNo());
        return channelService.query(request);
    }

    /** 取消支付（持单锁）：未支付→关渠道+补偿资产段+关单；渠道已支付→补单完成 */
    @Lock4j(name = "order", keys = "#orderNo")

    @Override
    public String cancel(Long userId, String orderNo) {
        return doCancel(userId, orderNo);
    }

    /** 超时关单入口（持单锁，供 CloseTask 逐单调用）：锁内重读，仍是 INIT/PAYING 才处理 */
    @Lock4j(name = "order", keys = "#orderNo")
    public void closeExpired(String orderNo) {
        PayOrder fresh = payOrderMapper.findByOrderNo(orderNo);
        if (fresh != null && OrderStateMachine.INSTANCE.canTransition(fresh.getState(), OrderEvent.CLOSE)) {
            closeOrFinish(fresh);
        }
    }

    private String doCancel(Long userId, String orderNo) {
        PayOrder order = payOrderMapper.findByOrderNo(orderNo);
        validatorChain.validate(PayValidationContext.forOrder(PayScene.CANCEL, userId, orderNo, order));
        OrderState state = order.getState();
        if (state == OrderState.SUCCESS) {
            throw new CommonException(ErrorCode.ORDER_PAID, orderNo);
        }
        if (state == OrderState.CLOSED || state == OrderState.FAIL) {
            return state.name();
        }
        OrderState result = closeOrFinish(order);
        if (result == OrderState.SUCCESS) {
            // 取消时发现分段/渠道已实付，已补单完成，不能关
            throw new CommonException(ErrorCode.ORDER_PAID, orderNo);
        }
        return result.name();
    }

    /**
     * 关闭或补单（取消与超时关单共用）。
     *
     * <p><b>必须先尝试结单再考虑关闭</b>：崩溃恢复窗口下可能出现"分段已全部 SUCCESS
     * 但主单停在 PAYING"（回调把渠道段推成功后、结单前进程崩溃）。此时渠道款已实收，
     * 若直接走回滚+关单，渠道那笔钱既不退也不入账——资损。所以进来先补单，
     * 补单成功直接返回 SUCCESS。</p>
     *
     * @return 处理后的主单终态：SUCCESS（补单完成）或 CLOSED（已关闭）
     */
    public OrderState closeOrFinish(PayOrder order) {
        String orderNo = order.getOrderNo();

        // 1. 先尝试结单（幂等，分段未全成功时无副作用）
        orderFinisher.finishIfAllSuccess(orderNo);
        PayOrder fresh = payOrderMapper.findByOrderNo(orderNo);
        if (fresh.getState() == OrderState.SUCCESS) {
            log.info("关单时发现分段已全部成功，补单完成, orderNo={}", orderNo);
            return OrderState.SUCCESS;
        }

        List<PayPart> parts = payPartMapper.findByOrderNo(orderNo);
        PayPart channelPart = channelPartOf(parts);

        if (channelPart != null && !channelPart.getState().isTerminal()) {
            // 2. 查证渠道是否已支付（内核在已支付时会推进分段 SUCCESS 并触发监听）
            boolean channelPaid = queryChannelPaid(channelPart);
            if (channelPaid) {
                orderFinisher.finishIfAllSuccess(orderNo);
                log.info("关单时查证渠道已支付，走补单完成, orderNo={}", orderNo);
                return OrderState.SUCCESS;
            }
            // 3. 未支付：关闭渠道交易（内核会查证并关渠道、本地分段推进 CLOSED）
            try {
                channelService.cancel(channelPart.getChannelCode(), orderNo, channelPart.getPartNo());
            } catch (ChannelException e) {
                if (e.error() == ErrorCode.ORDER_HAS_PAID) {
                    // 关闭时渠道侧已支付：补单完成
                    payPartMapper.changeState(channelPart.getPartNo(), channelPart.getState(), PartState.SUCCESS);
                    orderFinisher.finishIfAllSuccess(orderNo);
                    return OrderState.SUCCESS;
                }
                log.warn("渠道关闭异常，继续本地关闭, orderNo={}, err={}", orderNo, e.getMessage());
                payPartMapper.changeState(channelPart.getPartNo(), channelPart.getState(), PartState.CLOSED);
            }
        }

        // 4. 资产段补偿返还 + 其余未终态分段关闭 + 主单关闭
        assetPartService.rollbackAssetParts(orderNo, order.getUserId());
        for (PayPart part : payPartMapper.findByOrderNo(orderNo)) {
            if (PartStateMachine.INSTANCE.canTransition(part.getState(), PartEvent.CLOSE)) {
                payPartMapper.changeState(part.getPartNo(), part.getState(), PartState.CLOSED);
            }
        }
        if (payOrderMapper.markClosed(orderNo, fresh.getState(), OrderState.CLOSED, LocalDateTime.now()) == 1) {
            events.publishEvent(new OrderClosedEvent(orderNo, order.getUserId()));
        }
        return OrderState.CLOSED;
    }

    /**
     * 查证渠道是否已支付。
     * 只对 PAYING 段查渠道（INIT 段从未发起渠道支付，必然未支付；
     * 内核 query 对 INIT 段也会返回 true，因为 INIT 无法直接转 SUCCESS，会被当成"终态"）。
     * 内核 query 在已支付时会顺手把分段推进 SUCCESS 并触发监听。
     */
    private boolean queryChannelPaid(PayPart channelPart) {
        if (channelPart.getState() != PartState.PAYING
            || !channelService.supports(channelPart.getChannelCode(), ActionType.QUERY)) {
            return false;
        }
        try {
            return channelService.query(
                new QueryRequest(channelPart.getChannelCode(), channelPart.getOrderNo(),
                    channelPart.getPartNo(), channelPart.getThirdNo()));
        } catch (ChannelException e) {
            log.warn("关单查证渠道失败, partNo={}, err={}", channelPart.getPartNo(), e.getMessage());
            return false;
        }
    }

    /** 支付单详情 */
    @Override
    public OrderDetail detail(Long userId, String orderNo) {
        PayOrder order = payOrderMapper.findByOrderNo(orderNo);
        validatorChain.validate(PayValidationContext.forOrder(PayScene.DETAIL, userId, orderNo, order));
        return new OrderDetail(toOrderView(order),
            payPartMapper.findByOrderNo(orderNo).stream().map(this::toPartView).toList());
    }

    /** 实体 → 跨模块视图（契约数据模型不暴露 MyBatis 实体）。 */
    private PayOrderView toOrderView(PayOrder o) {
        return new PayOrderView(o.getId(), o.getOrderNo(), o.getAppId(), o.getBizOrderNo(), o.getUserId(),
            o.getTotalAmount(), o.getCurrency(), o.getState().name(), o.getExpireTime(), o.getPayTime(),
            o.getCloseTime(), o.getRefundableAmount(), o.getRefundedAmount(), o.getFailReason(),
            o.getCreateTime(), o.getUpdateTime());
    }

    private PayPartView toPartView(PayPart p) {
        return new PayPartView(p.getId(), p.getPartNo(), p.getOrderNo(), p.getUserId(), p.getPayType(),
            p.getAmount(), p.getPointCount(), p.getUserCouponId(), p.getChannelCode(), p.getThirdNo(),
            p.getChannelPayload(), p.getState().name(), p.getRefundedAmount(), p.getPayTime(),
            p.getCreateTime(), p.getUpdateTime());
    }

    private PayPart channelPartOf(List<PayPart> parts) {
        for (PayPart part : parts) {
            if (part.getPayType() == PayType.CHANNEL) {
                return part;
            }
        }
        return null;
    }

    private void markPartsFailed(List<PayPart> parts) {
        for (PayPart part : parts) {
            if (part.getState() == PartState.INIT) {
                payPartMapper.changeState(part.getPartNo(), PartState.INIT, PartState.FAIL);
            }
        }
    }
}
