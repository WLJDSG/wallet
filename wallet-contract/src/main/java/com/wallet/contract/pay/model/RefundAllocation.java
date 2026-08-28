package com.wallet.contract.pay.model;

/**
 * 退款分摊结果：一段退款（金额分 + 积分按比例折算）。
 *
 * @param part       原支付分段视图
 * @param amount     本段退款金额，单位分
 * @param pointCount 本段退款积分
 */
public record RefundAllocation(PayPartView part, long amount, long pointCount) {
}
