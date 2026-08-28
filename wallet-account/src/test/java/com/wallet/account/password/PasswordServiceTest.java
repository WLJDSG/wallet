package com.wallet.account.password;

import com.wallet.account.config.PasswordProperties;
import com.wallet.account.entity.PayPassword;
import com.wallet.common.error.ErrorCode;
import com.wallet.account.serviceImpl.password.PasswordServiceImpl;
import com.wallet.account.serviceImpl.password.PasswordStore;
import com.wallet.common.error.CommonException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * 支付密码服务单测：内存存储 + 本地 Redis。
 * 需要 127.0.0.1:6379 Redis，连不上时跳过。
 */
class PasswordServiceTest {

    private static RedissonClient redisson;
    private static PasswordProperties props;

    @BeforeAll
    static void setUpRedis() {
        try {
            Config config = new Config();
            config.useSingleServer()
                .setAddress("redis://127.0.0.1:6379")
                .setConnectTimeout(1000)
                .setTimeout(1000);
            redisson = Redisson.create(config);
        } catch (Exception e) {
            assumeTrue(false, "本地 Redis 不可用，跳过支付密码测试: " + e.getMessage());
        }
        props = new PasswordProperties();
        props.setMaxContinuousFail(3);
        props.setMaxDailyFail(5);
        props.setLockMinutes(10);
        props.setTicketTtlSeconds(60);
    }

    @AfterAll
    static void tearDown() {
        if (redisson != null) {
            redisson.shutdown();
        }
    }

    private MemoryPasswordStore store;
    private PasswordServiceImpl service;

    @BeforeEach
    void setUp() {
        store = new MemoryPasswordStore();
        service = new PasswordServiceImpl(store, redisson, props);
    }

    @Test
    void setThenVerifyAndConsume() {
        service.set(1001L, "123456", null);
        String ticket = service.verifyAndIssue(1001L, "123456", "O1", 10000);
        assertNotNull(ticket);
        service.consumeTicket(ticket, 1001L, "O1", 10000); // 不抛异常即通过
    }

    @Test
    void wrongPasswordThrows() {
        service.set(1002L, "123456", null);
        CommonException e = assertThrows(CommonException.class,
            () -> service.verifyAndIssue(1002L, "wrong", "O1", 10000));
        assertEquals(ErrorCode.PASSWORD_WRONG.code(), e.getCode());
    }

    @Test
    void continuousFailsLockOut() {
        service.set(1003L, "123456", null);
        for (int i = 0; i < 3; i++) {
            try {
                service.verifyAndIssue(1003L, "bad", "O1", 10000);
            } catch (CommonException expected) {
                // 连续失败，忽略
            }
        }
        CommonException e = assertThrows(CommonException.class,
            () -> service.verifyAndIssue(1003L, "123456", "O1", 10000));
        assertEquals(ErrorCode.PASSWORD_LOCKED.code(), e.getCode(), "连续 3 次错误后即使密码正确也应锁定");
    }

    @Test
    void ticketCanOnlyBeConsumedOnce() {
        service.set(1004L, "123456", null);
        String ticket = service.verifyAndIssue(1004L, "123456", "O1", 10000);
        service.consumeTicket(ticket, 1004L, "O1", 10000);
        CommonException e = assertThrows(CommonException.class,
            () -> service.consumeTicket(ticket, 1004L, "O1", 10000));
        assertEquals(ErrorCode.TICKET_INVALID.code(), e.getCode(), "票据只能消费一次");
    }

    @Test
    void ticketRejectsMismatchedOrder() {
        service.set(1005L, "123456", null);
        String ticket = service.verifyAndIssue(1005L, "123456", "O1", 10000);
        CommonException e = assertThrows(CommonException.class,
            () -> service.consumeTicket(ticket, 1005L, "O2", 10000));
        assertEquals(ErrorCode.TICKET_INVALID.code(), e.getCode(), "订单号不匹配应拒绝");
    }

    @Test
    void verifyWithoutSettingThrows() {
        CommonException e = assertThrows(CommonException.class,
            () -> service.verifyAndIssue(1006L, "123456", "O1", 10000));
        assertEquals(ErrorCode.PASSWORD_NOT_SET.code(), e.getCode());
    }

    /** 内存版密码存储 */
    static class MemoryPasswordStore implements PasswordStore {
        private final Map<Long, PayPassword> map = new ConcurrentHashMap<>();

        @Override
        public PayPassword findByUserId(Long userId) {
            return map.get(userId);
        }

        @Override
        public void insert(PayPassword password) {
            map.put(password.getUserId(), password);
        }

        @Override
        public void updateHash(Long userId, String newHash, int newVersion) {
            PayPassword existing = map.get(userId);
            assertTrue(existing != null, "更新哈希前应有记录");
            existing.setPasswordHash(newHash);
            existing.setVersion(newVersion);
        }
    }
}
