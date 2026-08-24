package com.wallet.pay.event;

/**
 * 领域事件：支付单支付成功。主单被 CAS 推进 SUCCESS（影响行数=1）后发布，至多一次。
 *
 * <p>事件在持单锁内同步派发，监听器只做轻量动作（记录、发 MQ、改缓存）；
 * 重活用 @Async 或转投消息队列，勿在监听器里做慢 IO 拖长持锁时间。</p>
 */
public record OrderPaidEvent(String orderNo, Long userId, long totalAmount) {
}
