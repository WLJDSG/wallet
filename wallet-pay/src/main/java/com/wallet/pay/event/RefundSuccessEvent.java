package com.wallet.pay.event;

/**
 * 领域事件：退款成功（三方 + 资产全部退完，退款单已 CAS 推进 SUCCESS）。
 * 至多一次。派发约束同 {@link OrderPaidEvent}。
 */
public record RefundSuccessEvent(String refundNo, String orderNo, Long userId, long amount) {
}
