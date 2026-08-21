package com.wallet.app.model;

/**
 * 发起退款请求。
 *
 * @param orderNo 支付单号
 * @param amount  退款金额，单位分（按 CHANNEL→MONEY→POINT 分摊）
 * @param reason  退款原因
 */
public record RefundReq(String orderNo, long amount, String reason) {
}
