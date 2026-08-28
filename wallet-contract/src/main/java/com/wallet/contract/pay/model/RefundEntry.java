package com.wallet.contract.pay.model;

/**
 * 退款分摊结果 + 退款分段号（退款编排与资产事务共用）。
 */
public record RefundEntry(RefundAllocation alloc, String refundPartNo) {
}
