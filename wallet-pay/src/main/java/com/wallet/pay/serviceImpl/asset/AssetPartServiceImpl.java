package com.wallet.pay.serviceImpl.asset;

import com.wallet.contract.account.CouponService;
import com.wallet.contract.account.MoneyService;
import com.wallet.contract.account.PointService;
import com.wallet.contract.pay.AssetPartService;
import com.wallet.contract.pay.model.PayOrderView;
import com.wallet.contract.pay.model.PayPartView;
import com.wallet.contract.pay.model.RefundEntry;
import com.wallet.common.error.CommonException;
import com.wallet.common.error.ErrorCode;
import com.wallet.pay.entity.PayPart;
import com.wallet.common.enums.PayType;
import com.wallet.pay.mapper.PayOrderMapper;
import com.wallet.pay.mapper.PayPartMapper;
import com.wallet.pay.mapper.RefundOrderMapper;
import com.wallet.pay.mapper.RefundPartMapper;
import com.wallet.common.enums.PartState;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.wallet.common.enums.RefundState;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 资产分段的扣减与补偿回滚——事务边界所在。
 *
 * <p>必须是独立 Bean：PayService 若在自身内部 this. 调用 @Transactional 方法，
 * Spring 代理不生效、事务不会开启（之前的 bug）。经这里调用，
 * "任一资产段失败整体回滚"才真正成立。</p>
 */
@Slf4j
@Service
@AllArgsConstructor
public class AssetPartServiceImpl implements AssetPartService {

    private final PayPartMapper payPartMapper;
    private final PayOrderMapper payOrderMapper;
    private final RefundPartMapper refundPartMapper;
    private final RefundOrderMapper refundOrderMapper;
    private final MoneyService moneyService;
    private final PointService pointService;
    private final CouponService couponService;


    /** 扣资产段：一个本地事务内按 券→积分→余额 顺序扣减，任一失败整体回滚 */
    @Transactional
    public void deductAssetParts(Long userId, List<PayPartView> parts, String orderNo) {
        LocalDateTime now = LocalDateTime.now();
        for (PayPartView part : parts) {
            switch (part.payType()) {
                case COUPON -> {
                    couponService.use(userId, part.userCouponId(), orderNo);
                    payPartMapper.markAssetDone(part.partNo(), PartState.INIT, PartState.SUCCESS, now);
                }
                case POINT -> {
                    pointService.pay(userId, part.pointCount(), part.partNo(), orderNo, "支付");
                    payPartMapper.markAssetDone(part.partNo(), PartState.INIT, PartState.SUCCESS, now);
                }
                case MONEY -> {
                    moneyService.pay(userId, part.amount(), part.partNo(), orderNo, "支付");
                    payPartMapper.markAssetDone(part.partNo(), PartState.INIT, PartState.SUCCESS, now);
                }
                case CHANNEL -> {
                    // 三方段不在这里处理
                }
            }
        }
    }

    /** 补偿回滚已扣资产段（SUCCESS→ROLLBACK + 逆向流水 + 还券） */
    @Transactional
    public void rollbackAssetParts(String orderNo, Long orderUserId) {
        for (PayPart part : payPartMapper.findByOrderNo(orderNo)) {
            if (part.getState() != PartState.SUCCESS) {
                continue;
            }
            switch (part.getPayType()) {
                case COUPON -> couponService.restore(orderUserId, part.getUserCouponId(), orderNo);
                case POINT -> pointService.rollback(orderUserId, part.getPointCount(),
                    part.getPartNo(), orderNo, "支付未完成回滚");
                case MONEY -> moneyService.rollback(orderUserId, part.getAmount(),
                    part.getPartNo(), orderNo, "支付未完成回滚");
                case CHANNEL -> {
                    // 三方段不回滚
                }
            }
            payPartMapper.changeState(part.getPartNo(), PartState.SUCCESS, PartState.ROLLBACK);
        }
    }

    /** 资产退款 + 券返还 + 扣主单可退（一个本地事务；三方分段已由内核处理，此处跳过） */
    @Transactional
    public void refundAssets(Long userId, PayOrderView order, List<RefundEntry> entries, String refundNo) {
        String orderNo = order.orderNo();
        long totalRefund = 0;
        for (RefundEntry entry : entries) {
            totalRefund += entry.alloc().amount();
            switch (entry.alloc().part().payType()) {
                case MONEY -> {
                    moneyService.refund(userId, entry.alloc().amount(), entry.refundPartNo(), orderNo, "退款");
                    refundPartMapper.changeState(entry.refundPartNo(), RefundState.INIT, RefundState.SUCCESS);
                }
                case POINT -> {
                    if (entry.alloc().pointCount() > 0) {
                        pointService.refund(userId, entry.alloc().pointCount(), entry.refundPartNo(), orderNo,
                            "退款");
                    }
                    refundPartMapper.changeState(entry.refundPartNo(), RefundState.INIT, RefundState.SUCCESS);
                }
                default -> {
                    // 三方分段已由内核处理；券段不在此处理
                }
            }
        }
        // 券返还：仅当累计退款达到"除券外全退"
        long couponAmount = 0;
        List<PayPart> parts = payPartMapper.findByOrderNo(orderNo);
        for (PayPart part : parts) {
            if (part.getPayType() == PayType.COUPON) {
                couponAmount += part.getAmount();
            }
        }
        if (order.refundedAmount() + totalRefund >= order.totalAmount() - couponAmount) {
            boolean restored = false;
            for (PayPart part : parts) {
                if (part.getPayType() == PayType.COUPON && part.getState() == PartState.SUCCESS) {
                    restored = couponService.restore(order.userId(), part.getUserCouponId(), orderNo);
                }
            }
            refundOrderMapper.updateCouponBack(refundNo, restored ? 1 : 0);
        }
        // 资金 CAS 必须校验影响行数：可退不足说明并发前提被破坏，抛异常回滚整个退款事务
        if (payOrderMapper.reduceRefundable(orderNo, totalRefund) != 1) {
            throw new CommonException(ErrorCode.REFUND_TOO_MUCH,
                "可退金额不足（并发校验），orderNo=" + orderNo + ", 申请=" + totalRefund);
        }
    }
}
