package com.wallet.pay.service;

import com.wallet.pay.entity.PayPart;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 退款分摊测试：CHANNEL→MONEY→POINT 逆序、每段上限=段金额-已退、券不参与、超额拒绝。
 */
class RefundSplitterTest {

    private PayPart part(String partNo, String payType, long amount, long refunded) {
        PayPart part = new PayPart();
        part.setPartNo(partNo);
        part.setPayType(payType);
        part.setAmount(amount);
        part.setRefundedAmount(refunded);
        part.setPointCount(payType.equals("POINT") ? amount : null); // 1分=1积分简化
        return part;
    }

    @Test
    void channelAbsorbsFirst() {
        List<PayPart> parts = List.of(
            part("T1", "COUPON", 1000, 0),
            part("T2", "MONEY", 2000, 0),
            part("T3", "CHANNEL", 7000, 0));
        List<RefundSplitter.Alloc> allocs = RefundSplitter.split(parts, 3000);
        assertEquals(1, allocs.size());
        assertEquals("T3", allocs.get(0).part().getPartNo());
        assertEquals(3000, allocs.get(0).amount());
    }

    @Test
    void spillsToMoneyThenPoint() {
        List<PayPart> parts = List.of(
            part("T1", "POINT", 2000, 0),
            part("T2", "MONEY", 3000, 0),
            part("T3", "CHANNEL", 5000, 0));
        List<RefundSplitter.Alloc> allocs = RefundSplitter.split(parts, 7000);
        // CHANNEL 吸收 5000，剩 2000 → MONEY 吸收 2000
        assertEquals(2, allocs.size());
        assertEquals("T3", allocs.get(0).part().getPartNo());
        assertEquals(5000, allocs.get(0).amount());
        assertEquals("T2", allocs.get(1).part().getPartNo());
        assertEquals(2000, allocs.get(1).amount());
    }

    @Test
    void respectsAlreadyRefunded() {
        List<PayPart> parts = List.of(
            part("T1", "CHANNEL", 5000, 3000));
        List<RefundSplitter.Alloc> allocs = RefundSplitter.split(parts, 2000);
        assertEquals(1, allocs.size());
        assertEquals(2000, allocs.get(0).amount());
    }

    @Test
    void pointCountConvertedFromPartRatio() {
        PayPart pointPart = part("T1", "POINT", 1000, 0);
        pointPart.setPointCount(100L); // 1000分 = 100积分，即 10分=1积分
        List<RefundSplitter.Alloc> allocs = RefundSplitter.split(List.of(pointPart), 300);
        assertEquals(30, allocs.get(0).pointCount());
    }

    @Test
    void couponNotAllocated() {
        List<PayPart> parts = List.of(
            part("T1", "COUPON", 500, 0),
            part("T2", "MONEY", 500, 0));
        List<RefundSplitter.Alloc> allocs = RefundSplitter.split(parts, 300);
        assertEquals(1, allocs.size());
        assertEquals("T2", allocs.get(0).part().getPartNo());
    }

    @Test
    void overAmountThrows() {
        List<PayPart> parts = List.of(part("T1", "MONEY", 100, 0));
        assertThrows(IllegalArgumentException.class, () -> RefundSplitter.split(parts, 200));
    }

    @Test
    void fullAmountExhaustsAll() {
        List<PayPart> parts = List.of(
            part("T1", "POINT", 2000, 0),
            part("T2", "MONEY", 3000, 0),
            part("T3", "CHANNEL", 5000, 0));
        List<RefundSplitter.Alloc> allocs = RefundSplitter.split(parts, 10000);
        assertEquals(3, allocs.size());
        assertTrue(allocs.stream().mapToLong(RefundSplitter.Alloc::amount).sum() == 10000);
    }
}
