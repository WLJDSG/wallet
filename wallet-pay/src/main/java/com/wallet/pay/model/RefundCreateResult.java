package com.wallet.pay.model;

/**
 * 创建退款结果。
 *
 * @param refundNo 退款单号
 * @param state    退款单状态
 */
public record RefundCreateResult(String refundNo, String state) {
}
