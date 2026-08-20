package com.zbkj.paychannel.spi;

import java.util.function.Supplier;

/**
 * 分布式锁 SPI（宿主实现，通常适配 Lock4j / Redisson）。
 *
 * <p>编排层用它保证同一订单的支付/回调/查询/退款串行化。
 * 实现必须是阻塞获取（可带超时），获取失败抛异常而不是静默跳过。</p>
 */
public interface PayLockManager {

    /**
     * 在锁内执行。
     *
     * @param lockKey 锁键（编排层已拼好，如 "pay:channel:order:{orderNo}"）
     * @param action  锁内动作
     */
    <T> T withLock(String lockKey, Supplier<T> action);
}
