package com.wallet.common.lock;

import com.wallet.common.error.BizException;
import com.wallet.common.error.CommonError;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 分布式锁（Redisson 可重入锁）。
 *
 * <p>整个钱包只有一种锁用法：同一支付单的所有状态变更（提交、回调、查询、退款、取消、关单）
 * 都用 {@link #payOrderKey(String)} 生成的同一把锁，保证互相串行。</p>
 *
 * <p>实现要点：</p>
 * <ul>
 *   <li>tryLock 等待 3 秒，拿不到快速失败（回调方会重试、任务下一轮再来）；</li>
 *   <li>不传 leaseTime，启用 Redisson 看门狗自动续期——持锁期间做渠道 HTTP 调用也不会锁过期；</li>
 *   <li>可重入：锁内回调链路再次进入锁内方法不会死锁。</li>
 * </ul>
 */
public class LockService {

    private static final long WAIT_SECONDS = 3;

    private final RedissonClient redisson;

    public LockService(RedissonClient redisson) {
        this.redisson = redisson;
    }

    /** 同一支付单的唯一锁 key */
    public static String payOrderKey(String orderNo) {
        return "wallet:lock:order:" + orderNo;
    }

    public <T> T withLock(String key, Supplier<T> action) {
        RLock lock = redisson.getLock(key);
        boolean locked;
        try {
            locked = lock.tryLock(WAIT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BizException(CommonError.LOCK_FAILED, key);
        }
        if (!locked) {
            throw new BizException(CommonError.LOCK_FAILED, key);
        }
        try {
            return action.get();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /** 无返回值版本 */
    public void withLock(String key, Runnable action) {
        withLock(key, () -> {
            action.run();
            return null;
        });
    }
}
