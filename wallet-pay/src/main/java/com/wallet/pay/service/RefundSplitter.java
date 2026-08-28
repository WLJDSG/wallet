package com.wallet.pay.service;

import com.wallet.pay.entity.PayPart;
import com.wallet.contract.pay.enums.PayType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 退款分摊（纯函数）：按 CHANNEL → MONEY → POINT 逆序分摊申请金额，
 * 每段最多吸收"本段金额 - 已退金额"。券段不折现、不参与分摊。
 */
public final class RefundSplitter {

    private RefundSplitter() {
    }

    /** 分摊结果：一段退款 */
    public record Alloc(PayPart part, long amount, long pointCount) {
    }

    /**
     * @param parts  支付单全部分段
     * @param amount 申请退款金额（分）
     * @return 分摊明细；金额不够时抛 IllegalArgumentException
     */
    public static List<Alloc> split(List<PayPart> parts, long amount) {
        long remaining = amount;
        List<Alloc> result = new ArrayList<>();
        List<PayPart> ordered = new ArrayList<>(parts);
        ordered.sort(Comparator.comparingInt(RefundSplitter::priority));

        for (PayPart part : ordered) {
            if (remaining == 0) {
                break;
            }
            if (part.getPayType() == PayType.COUPON) {
                continue; // 券不折现、不参与退款分摊
            }
            long available = part.getAmount() - part.getRefundedAmount();
            if (available <= 0) {
                continue;
            }
            long take = Math.min(available, remaining);
            long pointCount = part.getPointCount() == null || part.getPointCount() <= 0 ? 0
                : Math.round((double) part.getPointCount() * take / part.getAmount());
            result.add(new Alloc(part, take, pointCount));
            remaining -= take;
        }
        if (remaining > 0) {
            throw new IllegalArgumentException("可退金额不足，缺 " + remaining + " 分");
        }
        return result;
    }

    /** 分摊优先级：CHANNEL 优先，其次 MONEY，再 POINT（券不参与分摊，只用于排序兜底） */
    private static int priority(PayPart part) {
        return switch (part.getPayType()) {
            case CHANNEL -> 0;
            case MONEY -> 1;
            case POINT -> 2;
            case COUPON -> 9;
        };
    }
}
