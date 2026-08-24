package com.wallet.pay.event;

/**
 * 领域事件：支付单关闭（超时关单/主动取消，资产段已补偿返还）。
 * 主单被 CAS 推进 CLOSED（影响行数=1）后发布，至多一次。派发约束同 {@link OrderPaidEvent}。
 */
public record OrderClosedEvent(String orderNo, Long userId) {
}
