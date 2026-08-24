package com.wallet.pay.service;

/**
 * 退款分摊结果 + 退款分段号（RefundService 与 AssetPartService 共用）。
 */
public record RefundEntry(RefundSplitter.Alloc alloc, String refundPartNo) {
}
