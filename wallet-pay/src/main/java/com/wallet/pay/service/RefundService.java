package com.wallet.pay.service;

import com.baomidou.lock.annotation.Lock4j;
import com.wallet.channel.ChannelKit;
import com.wallet.channel.enums.RefundState;
import com.wallet.channel.model.RefundRequest;
import com.wallet.common.error.BizException;
import com.wallet.common.util.IdMaker;
import com.wallet.pay.entity.PayOrder;
import com.wallet.pay.entity.PayPart;
import com.wallet.pay.entity.RefundOrder;
import com.wallet.pay.entity.RefundPart;
import com.wallet.pay.enums.PayType;
import com.wallet.pay.error.OrderError;
import com.wallet.pay.event.RefundSuccessEvent;
import com.wallet.pay.mapper.PayOrderMapper;
import com.wallet.pay.mapper.PayPartMapper;
import com.wallet.pay.mapper.RefundOrderMapper;
import com.wallet.pay.mapper.RefundPartMapper;
import com.wallet.pay.model.RefundCreateResult;
import com.wallet.pay.model.RefundDetail;
import com.wallet.pay.state.OrderState;
import com.wallet.pay.state.RefundOrderState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

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
@Slf4j
@Service
public class RefundService {

    private final RefundOrderMapper refundOrderMapper;
    private final RefundPartMapper refundPartMapper;
    private final PayOrderMapper payOrderMapper;
    private final PayPartMapper payPartMapper;
    private final ChannelKit channelKit;
    private final ApplicationEventPublisher events;
    private final AssetPartService assetPartService;
    private final TransactionTemplate transactionTemplate;

    public RefundService(RefundOrderMapper refundOrderMapper, RefundPartMapper refundPartMapper,
        PayOrderMapper payOrderMapper, PayPartMapper payPartMapper, ChannelKit channelKit,
        ApplicationEventPublisher events, AssetPartService assetPartService,
        TransactionTemplate transactionTemplate) {
        this.refundOrderMapper = refundOrderMapper;
        this.refundPartMapper = refundPartMapper;
        this.payOrderMapper = payOrderMapper;
        this.payPartMapper = payPartMapper;
        this.channelKit = channelKit;
        this.events = events;
        this.assetPartService = assetPartService;
        this.transactionTemplate = transactionTemplate;
    }

    /** 发起退款（持同一把支付单锁，与支付/回调互斥） */
    @Lock4j(name = "order", keys = "#orderNo")
    public RefundCreateResult create(Long userId, String orderNo, long amount, String reason) {
        return doCreate(userId, orderNo, amount, reason);
    }

    private RefundCreateResult doCreate(Long userId, String orderNo, long amount, String reason) {
        PayOrder order = payOrderMapper.findByOrderNo(orderNo);
        if (order == null) {
            throw new BizException(OrderError.ORDER_NOT_FOUND, orderNo);
        }
        if (!order.getUserId().equals(userId)) {
            throw new BizException(OrderError.ORDER_NOT_OWNED, orderNo);
        }
        if (order.getState() != OrderState.SUCCESS) {
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

        // 落退款单 + 退款分段（一个事务：不留半截 INIT 退款单；渠道调用在事务外）
        String refundNo = IdMaker.next("R");
        List<RefundEntry> entries = transactionTemplate.execute(status -> {
            LocalDateTime now = LocalDateTime.now();
            RefundOrder refundOrder = new RefundOrder();
            refundOrder.setRefundNo(refundNo);
            refundOrder.setOrderNo(orderNo);
            refundOrder.setUserId(userId);
            refundOrder.setRefundAmount(amount);
            refundOrder.setRefundPoint(0L);
            refundOrder.setCouponBack(0);
            refundOrder.setState(RefundOrderState.INIT);
            refundOrder.setReason(reason);
            refundOrder.setCreateTime(now);
            refundOrder.setUpdateTime(now);
            refundOrderMapper.insert(refundOrder);

            long refundPoint = 0;
            List<RefundEntry> list = new ArrayList<>();
            for (RefundSplitter.Alloc alloc : allocs) {
                String refundPartNo = IdMaker.next("RT");
                RefundPart refundPart = new RefundPart();
                refundPart.setRefundPartNo(refundPartNo);
                refundPart.setRefundNo(refundNo);
                refundPart.setPartNo(alloc.part().getPartNo());
                refundPart.setPayType(alloc.part().getPayType());
                refundPart.setAmount(alloc.amount());
                refundPart.setPointCount(alloc.pointCount());
                refundPart.setState(RefundState.INIT);
                refundPart.setCreateTime(now);
                refundPart.setUpdateTime(now);
                refundPartMapper.insert(refundPart);
                refundPoint += alloc.pointCount();
                list.add(new RefundEntry(alloc, refundPartNo));
            }
            refundOrderMapper.updateRefundPoint(refundNo, refundPoint);
            return list;
        });

        // 1. 先退三方
        RefundEntry channelEntry = findChannel(entries);
        if (channelEntry != null) {
            boolean ok = refundChannel(userId, order, channelEntry);
            if (!ok) {
                refundOrderMapper.changeState(refundNo, RefundOrderState.INIT, RefundOrderState.FAIL);
                log.warn("三方退款失败，退款单 FAIL, refundNo={}", refundNo);
                return new RefundCreateResult(refundNo, "FAIL");
            }
        }

        // 2. 退资产（一个本地事务，经 AssetPartService 代理调用，事务才生效）
        try {
            assetPartService.refundAssets(userId, order, entries, refundNo);
        } catch (RuntimeException e) {
            log.error("资产退款异常，退款单 FAIL, refundNo={}", refundNo, e);
            refundOrderMapper.changeState(refundNo, RefundOrderState.INIT, RefundOrderState.FAIL);
            return new RefundCreateResult(refundNo, "FAIL");
        }

        if (refundOrderMapper.markSuccess(refundNo, RefundOrderState.INIT, RefundOrderState.SUCCESS, LocalDateTime.now()) == 1) {
            events.publishEvent(new RefundSuccessEvent(refundNo, orderNo, userId, amount));
        }
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

    private RefundEntry findChannel(List<RefundEntry> entries) {
        for (RefundEntry entry : entries) {
            if (entry.alloc().part().getPayType() == PayType.CHANNEL) {
                return entry;
            }
        }
        return null;
    }
}
