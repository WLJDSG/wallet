package com.wallet.common.lock;

import com.wallet.common.error.BizException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * 需要本地 Redis（127.0.0.1:6379）。连不上时整组用例跳过，不算失败。
 */
class LockServiceTest {

    private static RedissonClient redisson;
    private static LockService lockService;

    @BeforeAll
    static void setUp() {
        try {
            Config config = new Config();
            config.useSingleServer()
                .setAddress("redis://127.0.0.1:6379")
                .setConnectTimeout(1000)
                .setTimeout(1000);
            redisson = Redisson.create(config);
            lockService = new LockService(redisson);
        } catch (Exception e) {
            assumeTrue(false, "本地 Redis 不可用，跳过锁测试: " + e.getMessage());
        }
    }

    @AfterAll
    static void tearDown() {
        if (redisson != null) {
            redisson.shutdown();
        }
    }

    @Test
    void reenterSameLockInSameThread() {
        String key = "wallet:test:reenter";
        String result = lockService.withLock(key, () -> lockService.withLock(key, () -> "inner"));
        assertEquals("inner", result);
    }

    @Test
    void secondThreadFailsAfterWaitTimeout() throws Exception {
        String key = "wallet:test:timeout";
        CountDownLatch holding = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        Thread holder = new Thread(() -> lockService.withLock(key, () -> {
            holding.countDown();
            try {
                // 持锁超过对方的 3 秒等待时间
                release.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return null;
        }));
        holder.start();
        assertTrue(holding.await(5, TimeUnit.SECONDS));

        AtomicReference<Exception> error = new AtomicReference<>();
        Thread waiter = new Thread(() -> {
            try {
                lockService.withLock(key, () -> "should not get here");
            } catch (BizException e) {
                error.set(e);
            }
        });
        waiter.start();
        waiter.join(8000);
        release.countDown();
        holder.join(5000);

        assertTrue(error.get() instanceof BizException, "等锁超时应抛 BizException(LOCK_FAILED)");
        assertEquals("LOCK_FAILED", ((BizException) error.get()).getCode());
    }

    @Test
    void lockMakesCounterSafe() throws Exception {
        String key = "wallet:test:counter";
        int threads = 8;
        int loops = 20;
        long[] counter = {0};
        CountDownLatch done = new CountDownLatch(threads);
        for (int i = 0; i < threads; i++) {
            new Thread(() -> {
                for (int j = 0; j < loops; j++) {
                    lockService.withLock(key, () -> {
                        counter[0]++;
                        return null;
                    });
                }
                done.countDown();
            }).start();
        }
        assertTrue(done.await(60, TimeUnit.SECONDS));
        assertEquals((long) threads * loops, counter[0]);
    }
}
