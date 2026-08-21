package com.wallet.pay.service;

import com.wallet.asset.service.CouponService;
import com.wallet.asset.service.MoneyService;
import com.wallet.asset.service.PointService;
import com.wallet.asset.service.password.PasswordService;
import com.wallet.channel.ChannelKit;
import com.wallet.channel.enums.ActionType;
import com.wallet.channel.enums.PayError;
import com.wallet.channel.error.ChannelException;
import com.wallet.channel.model.PayRequest;
import com.wallet.channel.model.PayResult;
import com.wallet.channel.model.QueryRequest;
import com.wallet.common.error.BizException;
import com.wallet.common.id.IdMaker;
import com.wallet.common.lock.LockService;
import com.wallet.pay.config.PayConfig;
import com.wallet.pay.entity.PayOrder;
import com.wallet.pay.entity.PayPart;
import com.wallet.pay.error.OrderError;
import com.wallet.pay.mapper.PayOrderMapper;
import com.wallet.pay.mapper.PayPartMapper;
import com.wallet.pay.model.CreateOrderCmd;
import com.wallet.pay.model.CreateOrderResult;
import com.wallet.pay.model.OrderDetail;
import com.wallet.pay.model.PartItem;
import com.wallet.pay.model.SubmitResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 支付编排服务。
 *
 * <p><b>同一把锁</b>：提交/回调/查询/取消/退款/关单全部经
 * {@link LockService#payOrderKey(String)} 加锁，同一支付单的所有状态变更串行执行。</p>
 *
 * <p>时序：创建（校验+落单）→ 提交（扣资产段在一个本地事务内，三方向发起在事务外）
 * → 回调/查询确认 → 全部成功推主单 SUCCESS；渠道下单失败或超时则补偿回滚资产段。</p>
 */
@Service
public class PayService {

    private static final Logger log = LoggerFactory.getLogger(PayService.class);

    public static final String TYPE_COUPON = "COUPON";
    public static final String TYPE_POINT = "POINT";
    public static final String TYPE_MONEY = "MONEY";
    public static final String TYPE_CHANNEL = "CHANNEL";

    private final PayOrderMapper payOrderMapper;
    private final PayPartMapper payPartMapper;
    private final MoneyService moneyService;
    private final PointService pointService;
    private final CouponService couponService;
    private final PasswordService passwordService;
    private final ChannelKit channelKit;
    private final LockService lockService;
    private final NotifyService notifyService;
    private final PayConfig config;

    public PayService(PayOrderMapper payOrderMapper, PayPartMapper payPartMapper, MoneyService moneyService,
        PointService pointService, CouponService couponService, PasswordService passwordService,
        ChannelKit channelKit, LockService lockService, NotifyService notifyService, PayConfig config) {
        this.payOrderMapper = payOrderMapper;
        this.payPartMapper = payPartMapper;
        this.moneyService = moneyService;
        this.pointService = pointService;
        this.couponService = couponService;
        this.passwordService = passwordService;
        this.channelKit = channelKit;
        this.lockService = lockService;
        this.notifyService = notifyService;
        this.config = config;
    }

    /** 创建支付单：校验分段合法性，落主单 + 全部分段（INIT）。不持锁（orderNo 新生成，靠唯一索引防重）。 */
    @Transactional
    public CreateOrderResult create(Long userId, CreateOrderCmd cmd) {
        long sum = 0;
        int channelCount = 0;
        for (PartItem item : cmd.parts()) {
            sum += item.amount();
            if (TYPE_CHANNEL.equals(item.payType())) {
                channelCount++;
            }
            validatePart(userId, item, cmd.totalAmount());
        }
        if (sum != cmd.totalAmount()) {
            throw new BizException(OrderError.AMOUNT_NOT_MATCH, "sum=" + sum + ", total=" + cmd.totalAmount());
        }
        if (channelCount > 1) {
            throw new BizException(OrderError.PART_INVALID, "一个支付单至多一个三方分段");
        }

        String orderNo = IdMaker.next("P");
        LocalDateTime now = LocalDateTime.now();
        PayOrder order = new PayOrder();
        order.setOrderNo(orderNo);
        order.setBizOrderNo(cmd.bizOrderNo());
        order.setUserId(userId);
        order.setTotalAmount(cmd.totalAmount());
        order.setCurrency(cmd.currency());
        order.setState("INIT");
        order.setExpireTime(now.plusMinutes(config.getExpireMinutes()));
        order.setRefundableAmount(0L);
        order.setRefundedAmount(0L);
        order.setCreateTime(now);
        order.setUpdateTime(now);
        payOrderMapper.insert(order);

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
            part.setState("INIT");
            part.setRefundedAmount(0L);
            part.setCreateTime(now);
            part.setUpdateTime(now);
            payPartMapper.insert(part);
        }
        return new CreateOrderResult(orderNo, order.getExpireTime());
    }

    private void validatePart(Long userId, PartItem item, long totalAmount) {
        switch (item.payType()) {
            case TYPE_COUPON -> {
                if (item.userCouponId() == null) {
                    throw new BizException(OrderError.PART_INVALID, "券段缺少 userCouponId");
                }
                var userCoupon = couponService.checkUsable(userId, item.userCouponId(), totalAmount);
                if (userCoupon.getFaceAmount() != item.amount()) {
                    throw new BizException(OrderError.PART_INVALID,
                        "券段金额必须等于面额 " + userCoupon.getFaceAmount());
                }
            }
            case TYPE_POINT -> {
                if (item.pointCount() == null || item.pointCount() <= 0) {
                    throw new BizException(OrderError.PART_INVALID, "积分段缺少 pointCount");
                }
                long expectAmount = item.pointCount() * 100 / config.getPointsPerYuan();
                if (expectAmount != item.amount()) {
                    throw new BizException(OrderError.PART_INVALID,
                        "积分段金额应为 " + expectAmount + "（" + item.pointCount() + " 积分按 "
                            + config.getPointsPerYuan() + " 积分/元折算）");
                }
            }
            case TYPE_MONEY -> {
                // 无需额外校验
            }
            case TYPE_CHANNEL -> {
                if (item.channelCode() == null || item.channelCode().trim().isEmpty()) {
                    throw new BizException(OrderError.PART_INVALID, "三方段缺少 channelCode");
                }
            }
            default -> throw new BizException(OrderError.PART_INVALID, "未知分段类型 " + item.payType());
        }
    }

    /** 提交支付：持单锁，扣资产段（事务）+ 发起三方（事务外）。 */
    public SubmitResult submit(Long userId, String orderNo, String ticket) {
        return lockService.withLock(LockService.payOrderKey(orderNo),
            () -> doSubmit(userId, orderNo, ticket));
    }

    private SubmitResult doSubmit(Long userId, String orderNo, String ticket) {
        PayOrder order = requireOwned(userId, orderNo);
        String state = order.getState();
        if ("SUCCESS".equals(state)) {
            return new SubmitResult("SUCCESS", null, "订单已支付");
        }
        if ("CLOSED".equals(state) || "FAIL".equals(state)) {
            throw new BizException(OrderError.ORDER_STATE_INVALID, "state=" + state);
        }

        List<PayPart> parts = payPartMapper.findByOrderNo(orderNo);

        if ("INIT".equals(state)) {
            // 含资产段的支付必须校验并消费支付密码授权票据
            if (hasAssetPart(parts)) {
                if (ticket == null || ticket.trim().isEmpty()) {
                    throw new BizException(OrderError.TICKET_REQUIRED, orderNo);
                }
                passwordService.consumeTicket(ticket, userId, orderNo, order.getTotalAmount());
            }
            // 主单 INIT→PAYING（CAS，防并发重复提交）
            if (payOrderMapper.changeState(orderNo, "INIT", "PAYING") == 0) {
                log.info("提交支付被并发处理, orderNo={}", orderNo);
                return submitRetry(order, parts);
            }
            // 扣资产段：一个本地事务，任一失败整体回滚
            try {
                deductAssetParts(userId, parts, orderNo);
            } catch (RuntimeException e) {
                log.warn("资产扣减失败, orderNo={}, err={}", orderNo, e.getMessage());
                payOrderMapper.markFailed(orderNo, "PAYING", "FAIL",
                    "资产扣减失败: " + e.getMessage());
                markPartsFailed(parts);
                throw e;
            }
            PayPart channelPart = channelPartOf(parts);
            if (channelPart == null) {
                // 纯资产支付：当场完成
                payOrderMapper.markPaid(orderNo, "PAYING", "SUCCESS", LocalDateTime.now(),
                    couponTotal(parts));
                log.info("纯资产支付完成, orderNo={}", orderNo);
                return new SubmitResult("SUCCESS", null, "支付成功");
            }
            return payChannel(userId, order, channelPart);
        }
        return submitRetry(order, parts);
    }

    /** 主单已是 PAYING（重复提交）：返回已有渠道支付参数或当前状态 */
    private SubmitResult submitRetry(PayOrder order, List<PayPart> parts) {
        if ("SUCCESS".equals(order.getState())) {
            return new SubmitResult("SUCCESS", null, "订单已支付");
        }
        PayPart channelPart = channelPartOf(parts);
        if (channelPart != null && "PAYING".equals(channelPart.getState())) {
            return new SubmitResult("PAYING", channelPart.getChannelPayload(), "支付已发起，等待结果");
        }
        if ("FAIL".equals(order.getState())) {
            throw new BizException(OrderError.ORDER_STATE_INVALID, "订单支付失败");
        }
        return new SubmitResult(order.getState(), null, "当前状态 " + order.getState());
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
            PayResult result = channelKit.flow().pay(request);
            payPartMapper.updatePayload(channelPart.getPartNo(), String.valueOf(result.channelPayload()));
            notifyService.scheduleAutoNotify(order.getOrderNo(), channelPart.getPartNo());
            log.info("渠道支付已发起, orderNo={}, partNo={}, queryable={}", order.getOrderNo(),
                channelPart.getPartNo(), result.queryable());
            return new SubmitResult("PAYING", result.channelPayload(), "支付已发起，等待结果");
        } catch (RuntimeException e) {
            log.warn("渠道下单失败，补偿回滚资产段, orderNo={}, err={}", order.getOrderNo(), e.getMessage());
            rollbackAssetParts(order.getOrderNo(), order.getUserId());
            payOrderMapper.markFailed(order.getOrderNo(), "PAYING", "FAIL", "渠道下单失败: " + e.getMessage());
            throw new BizException(OrderError.CHANNEL_PAY_FAILED, e.getMessage());
        }
    }

    /** 处理渠道异步回调（持单锁，内核不重复加锁） */
    public String handleCallback(String channelCode, String orderNo, String partNo, String body,
        Map<String, String> headers, String httpMethod, String requestUri) {
        return lockService.withLock(LockService.payOrderKey(orderNo), () -> {
            com.wallet.channel.model.CallbackRequest request =
                com.wallet.channel.model.CallbackRequest.builder()
                    .channelCode(channelCode)
                    .orderNo(orderNo)
                    .outTradeNo(partNo)
                    .httpMethod(httpMethod)
                    .requestUri(requestUri)
                    .headers(headers)
                    .body(body)
                    .build();
            return channelKit.flow().callback(request);
        });
    }

    /** 主动向渠道查证（持单锁） */
    public boolean query(Long userId, String orderNo) {
        return lockService.withLock(LockService.payOrderKey(orderNo), () -> doQuery(userId, orderNo));
    }

    private boolean doQuery(Long userId, String orderNo) {
        PayOrder order = requireOwned(userId, orderNo);
        if ("SUCCESS".equals(order.getState())) {
            return true;
        }
        PayPart channelPart = channelPartOf(payPartMapper.findByOrderNo(orderNo));
        if (channelPart == null) {
            return true; // 无三方段
        }
        if (!"INIT".equals(channelPart.getState()) && !"PAYING".equals(channelPart.getState())) {
            return true; // 分段已终态
        }
        QueryRequest request = new QueryRequest(channelPart.getChannelCode(), orderNo,
            channelPart.getPartNo(), channelPart.getThirdNo());
        return channelKit.flow().query(request);
    }

    /** 取消支付（持单锁）：未支付→关渠道+补偿资产段+关单；渠道已支付→补单完成 */
    public String cancel(Long userId, String orderNo) {
        return lockService.withLock(LockService.payOrderKey(orderNo), () -> doCancel(userId, orderNo));
    }

    private String doCancel(Long userId, String orderNo) {
        PayOrder order = requireOwned(userId, orderNo);
        String state = order.getState();
        if ("SUCCESS".equals(state)) {
            throw new BizException(OrderError.ORDER_PAID, orderNo);
        }
        if ("CLOSED".equals(state) || "FAIL".equals(state)) {
            return state;
        }
        closeOrFinish(order);
        return "CLOSED";
    }

    /**
     * 关闭或补单（取消与超时关单共用）：先查证三方，已支付→补单完成；未支付→关渠道+补偿资产段+关单。
     */
    public void closeOrFinish(PayOrder order) {
        String orderNo = order.getOrderNo();
        List<PayPart> parts = payPartMapper.findByOrderNo(orderNo);
        PayPart channelPart = channelPartOf(parts);

        if (channelPart != null && !isTerminal(channelPart.getState())) {
            // 查证渠道是否已支付（内核在已支付时会推进分段 SUCCESS 并触发监听）
            boolean channelPaid = queryChannelPaid(channelPart);
            if (channelPaid) {
                finishIfAllSuccess(orderNo);
                log.info("取消时发现渠道已支付，走补单完成, orderNo={}", orderNo);
                return;
            }
            // 未支付：关闭渠道交易（内核会查证并关渠道、本地分段推进 CLOSED）
            try {
                channelKit.flow().cancel(channelPart.getChannelCode(), orderNo, channelPart.getPartNo());
            } catch (ChannelException e) {
                if (e.error() == PayError.ORDER_HAS_PAID) {
                    // 关闭时渠道侧已支付：补单完成
                    payPartMapper.changeState(channelPart.getPartNo(), channelPart.getState(), "SUCCESS");
                    finishIfAllSuccess(orderNo);
                    return;
                }
                log.warn("渠道关闭异常，继续本地关闭, orderNo={}, err={}", orderNo, e.getMessage());
                payPartMapper.changeState(channelPart.getPartNo(), channelPart.getState(), "CLOSED");
            }
        }

        // 资产段补偿返还 + 其余未终态分段关闭
        rollbackAssetParts(orderNo, order.getUserId());
        for (PayPart part : payPartMapper.findByOrderNo(orderNo)) {
            if (!isTerminal(part.getState())) {
                payPartMapper.changeState(part.getPartNo(), part.getState(), "CLOSED");
            }
        }
        payOrderMapper.markClosed(orderNo, order.getState(), "CLOSED", LocalDateTime.now());
    }

    /**
     * 查证渠道是否已支付。
     * 只对 PAYING 段查渠道（INIT 段从未发起渠道支付，必然未支付；
     * 内核 query 对 INIT 段也会返回 true，因为 INIT 无法直接转 SUCCESS，会被当成"终态"）。
     * 内核 query 在已支付时会顺手把分段推进 SUCCESS 并触发监听。
     */
    private boolean queryChannelPaid(PayPart channelPart) {
        if (!"PAYING".equals(channelPart.getState())
            || !channelKit.supports(channelPart.getChannelCode(), ActionType.QUERY)) {
            return false;
        }
        try {
            return channelKit.flow().query(
                new QueryRequest(channelPart.getChannelCode(), channelPart.getOrderNo(),
                    channelPart.getPartNo(), channelPart.getThirdNo()));
        } catch (ChannelException e) {
            log.warn("关单查证渠道失败, partNo={}, err={}", channelPart.getPartNo(), e.getMessage());
            return false;
        }
    }

    /** 全部分段 SUCCESS 时把主单推进 SUCCESS（供监听器与补单路径共用） */
    public void finishIfAllSuccess(String orderNo) {
        boolean allSuccess = true;
        for (PayPart part : payPartMapper.findByOrderNo(orderNo)) {
            if (!"SUCCESS".equals(part.getState())) {
                allSuccess = false;
                break;
            }
        }
        if (allSuccess) {
            payOrderMapper.markPaid(orderNo, "PAYING", "SUCCESS", LocalDateTime.now(),
                couponTotal(payPartMapper.findByOrderNo(orderNo)));
            log.info("订单支付完成, orderNo={}", orderNo);
        }
    }

    /** 券面额合计（券段不折现，从可退金额中剔除） */
    private long couponTotal(List<PayPart> parts) {
        long total = 0;
        for (PayPart part : parts) {
            if ("COUPON".equals(part.getPayType())) {
                total += part.getAmount();
            }
        }
        return total;
    }

    /** 扣资产段：一个本地事务内按 券→积分→余额 顺序扣减，任一失败整体回滚 */
    @Transactional
    public void deductAssetParts(Long userId, List<PayPart> parts, String orderNo) {
        LocalDateTime now = LocalDateTime.now();
        for (PayPart part : parts) {
            switch (part.getPayType()) {
                case TYPE_COUPON -> {
                    couponService.use(userId, part.getUserCouponId(), orderNo);
                    payPartMapper.markAssetDone(part.getPartNo(), "INIT", "SUCCESS", now);
                }
                case TYPE_POINT -> {
                    pointService.pay(userId, part.getPointCount(), part.getPartNo(), orderNo, "支付");
                    payPartMapper.markAssetDone(part.getPartNo(), "INIT", "SUCCESS", now);
                }
                case TYPE_MONEY -> {
                    moneyService.pay(userId, part.getAmount(), part.getPartNo(), orderNo, "支付");
                    payPartMapper.markAssetDone(part.getPartNo(), "INIT", "SUCCESS", now);
                }
                default -> {
                    // 三方段不在这里处理
                }
            }
        }
    }

    /** 补偿回滚已扣资产段（SUCCESS→ROLLBACK + 逆向流水 + 还券） */
    @Transactional
    public void rollbackAssetParts(String orderNo, Long orderUserId) {
        for (PayPart part : payPartMapper.findByOrderNo(orderNo)) {
            if (!"SUCCESS".equals(part.getState())) {
                continue;
            }
            switch (part.getPayType()) {
                case TYPE_COUPON -> couponService.restore(orderUserId, part.getUserCouponId(), orderNo);
                case TYPE_POINT -> pointService.rollback(orderUserId, part.getPointCount(),
                    part.getPartNo(), orderNo, "支付未完成回滚");
                case TYPE_MONEY -> moneyService.rollback(orderUserId, part.getAmount(),
                    part.getPartNo(), orderNo, "支付未完成回滚");
                default -> {
                    // 三方段不回滚
                }
            }
            payPartMapper.changeState(part.getPartNo(), "SUCCESS", "ROLLBACK");
        }
    }

    /** 支付单详情 */
    public OrderDetail detail(Long userId, String orderNo) {
        PayOrder order = requireOwned(userId, orderNo);
        return new OrderDetail(order, payPartMapper.findByOrderNo(orderNo));
    }

    private PayOrder requireOwned(Long userId, String orderNo) {
        PayOrder order = payOrderMapper.findByOrderNo(orderNo);
        if (order == null) {
            throw new BizException(OrderError.ORDER_NOT_FOUND, orderNo);
        }
        if (!order.getUserId().equals(userId)) {
            throw new BizException(OrderError.ORDER_NOT_OWNED, orderNo);
        }
        return order;
    }

    private boolean hasAssetPart(List<PayPart> parts) {
        for (PayPart part : parts) {
            if (isAsset(part.getPayType())) {
                return true;
            }
        }
        return false;
    }

    private boolean isAsset(String payType) {
        return TYPE_COUPON.equals(payType) || TYPE_POINT.equals(payType) || TYPE_MONEY.equals(payType);
    }

    private PayPart channelPartOf(List<PayPart> parts) {
        for (PayPart part : parts) {
            if (TYPE_CHANNEL.equals(part.getPayType())) {
                return part;
            }
        }
        return null;
    }

    private void markPartsFailed(List<PayPart> parts) {
        for (PayPart part : parts) {
            if ("INIT".equals(part.getState())) {
                payPartMapper.changeState(part.getPartNo(), "INIT", "FAIL");
            }
        }
    }

    private boolean isTerminal(String state) {
        return "SUCCESS".equals(state) || "FAIL".equals(state) || "CLOSED".equals(state)
            || "ROLLBACK".equals(state);
    }
}
