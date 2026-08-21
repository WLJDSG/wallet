package com.wallet.pay.service;

import com.wallet.asset.service.CouponService;
import com.wallet.asset.service.MoneyService;
import com.wallet.asset.service.PointService;
import com.wallet.channel.ChannelKit;
import com.wallet.channel.model.RefundRequest;
import com.wallet.common.error.BizException;
import com.wallet.common.id.IdMaker;
import com.wallet.common.lock.LockService;
import com.wallet.pay.entity.PayOrder;
import com.wallet.pay.entity.PayPart;
import com.wallet.pay.entity.RefundOrder;
import com.wallet.pay.entity.RefundPart;
import com.wallet.pay.error.OrderError;
import com.wallet.pay.mapper.PayOrderMapper;
import com.wallet.pay.mapper.PayPartMapper;
import com.wallet.pay.mapper.RefundOrderMapper;
import com.wallet.pay.mapper.RefundPartMapper;
import com.wallet.pay.model.RefundCreateResult;
import com.wallet.pay.model.RefundDetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 退款服务。
 *
 * <p>分摊规则：按 CHANNEL → MONEY → POINT 逆序分摊；券不折现，仅当累计退款达到
 * "除券外全退"时返还券。执行顺序：先退三方（可能失败，失败整单 FAIL 资产分毫未动），
 * 后退资产（本地事务，几乎必成）。</p>
 */
@Service
public class RefundService {

    private static final Logger log = LoggerFactory.getLogger(RefundService.class);

    private final RefundOrderMapper refundOrderMapper;
    private final RefundPartMapper refundPartMapper;
    private final PayOrderMapper payOrderMapper;
    private final PayPartMapper payPartMapper;
    private final MoneyService moneyService;
    private final PointService pointService;
    private final CouponService couponService;
    private final ChannelKit channelKit;
    private final LockService lockService;

    public RefundService(RefundOrderMapper refundOrderMapper, RefundPartMapper refundPartMapper,
        PayOrderMapper payOrderMapper, PayPartMapper payPartMapper, MoneyService moneyService,
        PointService pointService, CouponService couponService, ChannelKit channelKit, LockService lockService) {
        this.refundOrderMapper = refundOrderMapper;
        this.refundPartMapper = refundPartMapper;
        this.payOrderMapper = payOrderMapper;
        this.payPartMapper = payPartMapper;
        this.moneyService = moneyService;
        this.pointService = pointService;
        this.couponService = couponService;
        this.channelKit = channelKit;
        this.lockService = lockService;
    }

    /** 发起退款（持同一把支付单锁，与支付/回调互斥） */
    public RefundCreateResult create(Long userId, String orderNo, long amount, String reason) {
        return lockService.withLock(LockService.payOrderKey(orderNo),
            () -> doCreate(userId, orderNo, amount, reason));
    }

    private RefundCreateResult doCreate(Long userId, String orderNo, long amount, String reason) {
        PayOrder order = payOrderMapper.findByOrderNo(orderNo);
        if (order == null) {
            throw new BizException(OrderError.ORDER_NOT_FOUND, orderNo);
        }
        if (!order.getUserId().equals(userId)) {
            throw new BizException(OrderError.ORDER_NOT_OWNED, orderNo);
        }
        if (!"SUCCESS".equals(order.getState())) {
            throw new BizException(OrderError.ORDER_NOT_PAID, orderNo);
        }
        if (amount <= 0) {
            throw new BizException(OrderError.REFUND_AMOUNT_INVALID, "amount=" + amount);
        }
        if (order.getRefundableAmount() < amount) {
            throw new BizException(OrderError.REFUND_TOO_MUCH,
                "申请 " + amount + "，可退 " + order.getRefundableAmount());
        }

        List<PayPart> parts = payPartMapper.findByOrderNo(orderNo);
        List<RefundSplitter.Alloc> allocs = RefundSplitter.split(parts, amount);

        // 落退款单 + 退款分段
        String refundNo = IdMaker.next("R");
        LocalDateTime now = LocalDateTime.now();
        RefundOrder refundOrder = new RefundOrder();
        refundOrder.setRefundNo(refundNo);
        refundOrder.setOrderNo(orderNo);
        refundOrder.setUserId(userId);
        refundOrder.setRefundAmount(amount);
        refundOrder.setRefundPoint(0L);
        refundOrder.setCouponBack(0);
        refundOrder.setState("INIT");
        refundOrder.setReason(reason);
        refundOrder.setCreateTime(now);
        refundOrder.setUpdateTime(now);
        refundOrderMapper.insert(refundOrder);

        long refundPoint = 0;
        List<RefundEntry> entries = new ArrayList<>();
        for (RefundSplitter.Alloc alloc : allocs) {
            String refundPartNo = IdMaker.next("RT");
            RefundPart refundPart = new RefundPart();
            refundPart.setRefundPartNo(refundPartNo);
            refundPart.setRefundNo(refundNo);
            refundPart.setPartNo(alloc.part().getPartNo());
            refundPart.setPayType(alloc.part().getPayType());
            refundPart.setAmount(alloc.amount());
            refundPart.setPointCount(alloc.pointCount());
            refundPart.setState("INIT");
            refundPart.setCreateTime(now);
            refundPart.setUpdateTime(now);
            refundPartMapper.insert(refundPart);
            refundPoint += alloc.pointCount();
            entries.add(new RefundEntry(alloc, refundPartNo));
        }
        refundOrderMapper.updateRefundPoint(refundNo, refundPoint);

        // 1. 先退三方
        RefundEntry channelEntry = findChannel(entries);
        if (channelEntry != null) {
            boolean ok = refundChannel(userId, order, channelEntry);
            if (!ok) {
                refundOrderMapper.changeState(refundNo, "INIT", "FAIL");
                log.warn("三方退款失败，退款单 FAIL, refundNo={}", refundNo);
                return new RefundCreateResult(refundNo, "FAIL");
            }
        }

        // 2. 退资产（一个本地事务）
        try {
            refundAssets(userId, order, entries, refundNo);
        } catch (RuntimeException e) {
            log.error("资产退款异常，退款单 FAIL, refundNo={}, err={}", refundNo, e.getMessage());
            refundOrderMapper.changeState(refundNo, "INIT", "FAIL");
            return new RefundCreateResult(refundNo, "FAIL");
        }

        refundOrderMapper.markSuccess(refundNo, "INIT", "SUCCESS", LocalDateTime.now());
        return new RefundCreateResult(refundNo, "SUCCESS");
    }

    /** 退款单详情 */
    public RefundDetail detail(Long userId, String refundNo) {
        RefundOrder refundOrder = refundOrderMapper.findByRefundNo(refundNo);
        if (refundOrder == null) {
            throw new BizException(OrderError.ORDER_NOT_FOUND, "refundNo=" + refundNo);
        }
        if (!refundOrder.getUserId().equals(userId)) {
            throw new BizException(OrderError.ORDER_NOT_OWNED, refundNo);
        }
        return new RefundDetail(refundOrder, refundPartMapper.findByRefundNo(refundNo));
    }

    private boolean refundChannel(Long userId, PayOrder order, RefundEntry entry) {
        PayPart channelPart = entry.alloc().part();
        RefundRequest request = RefundRequest.builder()
            .channelCode(channelPart.getChannelCode())
            .orderNo(order.getOrderNo())
            .outTradeNo(channelPart.getPartNo())
            .refundOrderNo(entry.refundPartNo())
            .amount(entry.alloc().amount())
            .userId(userId)
            .build();
        try {
            return channelKit.flow().refund(request);
        } catch (RuntimeException e) {
            log.warn("渠道退款异常, refundPartNo={}, err={}", entry.refundPartNo(), e.getMessage());
            return false;
        }
    }

    /** 资产退款 + 券返还 + 扣主单可退（一个本地事务） */
    @Transactional
    public void refundAssets(Long userId, PayOrder order, List<RefundEntry> entries, String refundNo) {
        String orderNo = order.getOrderNo();
        long totalRefund = 0;
        for (RefundEntry entry : entries) {
            totalRefund += entry.alloc().amount();
            if ("CHANNEL".equals(entry.alloc().part().getPayType())) {
                continue; // 三方分段已由内核处理
            }
            switch (entry.alloc().part().getPayType()) {
                case "MONEY" -> {
                    moneyService.refund(userId, entry.alloc().amount(), entry.refundPartNo(), orderNo, "退款");
                    refundPartMapper.changeState(entry.refundPartNo(), "INIT", "SUCCESS");
                }
                case "POINT" -> {
                    if (entry.alloc().pointCount() > 0) {
                        pointService.refund(userId, entry.alloc().pointCount(), entry.refundPartNo(), orderNo,
                            "退款");
                    }
                    refundPartMapper.changeState(entry.refundPartNo(), "INIT", "SUCCESS");
                }
                default -> {
                    // 券段不在此处理
                }
            }
        }
        // 券返还：仅当累计退款达到"除券外全退"
        long couponAmount = couponTotal(orderNo);
        if (order.getRefundedAmount() + totalRefund >= order.getTotalAmount() - couponAmount) {
            boolean restored = false;
            for (PayPart part : payPartMapper.findByOrderNo(orderNo)) {
                if ("COUPON".equals(part.getPayType()) && "SUCCESS".equals(part.getState())) {
                    restored = couponService.restore(order.getUserId(), part.getUserCouponId(), orderNo);
                }
            }
            refundOrderMapper.updateCouponBack(refundNo, restored ? 1 : 0);
        }
        payOrderMapper.reduceRefundable(orderNo, totalRefund);
    }

    private long couponTotal(String orderNo) {
        long total = 0;
        for (PayPart part : payPartMapper.findByOrderNo(orderNo)) {
            if ("COUPON".equals(part.getPayType())) {
                total += part.getAmount();
            }
        }
        return total;
    }

    private RefundEntry findChannel(List<RefundEntry> entries) {
        for (RefundEntry entry : entries) {
            if ("CHANNEL".equals(entry.alloc().part().getPayType())) {
                return entry;
            }
        }
        return null;
    }

    /** 分摊结果 + 退款分段号 */
    private record RefundEntry(RefundSplitter.Alloc alloc, String refundPartNo) {
    }
}
