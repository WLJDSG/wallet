package com.wallet.security.core;

import com.wallet.security.config.PaySecurityProperties;
import com.wallet.security.enums.LockoutScopeEnum;
import com.wallet.security.error.PaySecurityErrorCode;
import com.wallet.security.error.PaySecurityException;
import com.wallet.security.spi.PaySecurityKeyValueStore;

import java.time.ZoneId;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * 支付密码失败计数与锁定状态机。
 *
 * <p>余额支付与修改支付密码按 {@link LockoutScopeEnum} 分别计数、分别锁定：
 * 两个作用域使用不同 KV 键与锁定时长，一个作用域锁定不影响另一个作用域。
 * 连续错误计数按各自锁定窗口过期，每日累计计数在业务时区次日零点过期；
 * 任一计数达到阈值即写入解锁时间戳锁定对应功能。</p>
 */
public final class LockoutManager {

    private final PaySecurityKeyValueStore paySecurityKeyValueStore;
    private final RedisKeys redisKeys;
    private final PaySecurityProperties paySecurityProperties;
    private final ZoneId businessZone;

    public LockoutManager(PaySecurityKeyValueStore paySecurityKeyValueStore, RedisKeys redisKeys, PaySecurityProperties paySecurityProperties,
        ZoneId businessZone) {
        this.paySecurityKeyValueStore = paySecurityKeyValueStore;
        this.redisKeys = redisKeys;
        this.paySecurityProperties = paySecurityProperties;
        this.businessZone = businessZone;
    }

    /**
     * 记录一次密码校验失败并按阈值锁定对应作用域。
     *
     * @param scope 失败计数与锁定的作用域
     * @param uid 用户ID
     * @return 剩余可尝试次数，至少为 0
     */
    public int recordPasswordFailure(LockoutScopeEnum scope, Long uid) {
        // 连续失败计数随锁定窗口滑动过期（窗口内不再失败则自动归零）；当日累计计数到业务时区次日零点过期。
        int lockMinutes = lockMinutes(scope);
        long continuous = paySecurityKeyValueStore.increment(redisKeys.continuousFailure(scope, uid), 1L,
            TimeUnit.MINUTES.toSeconds(lockMinutes));
        long daily = paySecurityKeyValueStore.increment(redisKeys.dailyFailure(scope, uid), 1L,
            BusinessTime.secondsUntilTomorrow(businessZone));
        // 连续超限锁定固定分钟数；当日超限锁定到次日零点；两者同时命中取更晚的解锁时间。
        long lockedUntil = 0L;
        int continuousLimit = paySecurityProperties.getPasswordContinuousFailureLimit();
        int dailyLimit = paySecurityProperties.getPasswordDailyFailureLimit();
        if (continuous >= continuousLimit) {
            lockedUntil = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(lockMinutes);
        }
        if (daily >= dailyLimit) {
            lockedUntil = Math.max(lockedUntil, BusinessTime.startOfTomorrowMillis(businessZone));
        }
        // 锁值存解锁毫秒时间戳，key 的 TTL 与解锁时间对齐，读取侧无需额外清理。
        if (lockedUntil > 0L) {
            paySecurityKeyValueStore.set(redisKeys.lock(scope, uid), String.valueOf(lockedUntil),
                Math.max(1L, (lockedUntil - System.currentTimeMillis()) / 1000));
        }
        // 剩余可尝试次数取两种阈值余量的较小者，不为负；提示口径与实际锁定判定保持一致。
        return (int) Math.max(0L, Math.min(continuousLimit - continuous, dailyLimit - daily));
    }

    /**
     * 作用域锁定期间禁止对应功能的一切校验与票据消费。
     *
     * @param scope 锁定作用域
     * @param uid 用户ID
     */
    public void ensureNotLocked(LockoutScopeEnum scope, Long uid) {
        if (getLockedUntil(scope, uid) != null) {
            throw PaySecurityException.of(lockedError(scope));
        }
    }

    /**
     * 查询作用域锁定截止时间。
     *
     * @param scope 锁定作用域
     * @param uid 用户ID
     * @return 解锁时间；未锁定或锁已过期时返回 null（并顺带清理过期锁）
     */
    public Date getLockedUntil(LockoutScopeEnum scope, Long uid) {
        String value = paySecurityKeyValueStore.get(redisKeys.lock(scope, uid));
        if (value == null) {
            return null;
        }
        try {
            long timestamp = Long.parseLong(value);
            if (timestamp > System.currentTimeMillis()) {
                return new Date(timestamp);
            }
            paySecurityKeyValueStore.delete(redisKeys.lock(scope, uid));
            return null;
        } catch (NumberFormatException e) {
            paySecurityKeyValueStore.delete(redisKeys.lock(scope, uid));
            return null;
        }
    }

    /** 密码校验成功后清除对应作用域的连续失败计数（保留当日累计计数）。 */
    public void clearContinuousFailures(LockoutScopeEnum scope, Long uid) {
        paySecurityKeyValueStore.delete(redisKeys.continuousFailure(scope, uid));
    }

    /** 密码设置、修改或重置成功后清空全部作用域的失败计数与锁定。 */
    public void clearPasswordFailureState(Long uid) {
        for (LockoutScopeEnum scope : LockoutScopeEnum.values()) {
            paySecurityKeyValueStore.delete(redisKeys.continuousFailure(scope, uid));
            paySecurityKeyValueStore.delete(redisKeys.dailyFailure(scope, uid));
            paySecurityKeyValueStore.delete(redisKeys.lock(scope, uid));
        }
    }

    /** 作用域对应的连续失败锁定分钟数。 */
    private int lockMinutes(LockoutScopeEnum scope) {
        return scope == LockoutScopeEnum.PASSWORD_CHANGE
            ? paySecurityProperties.getPasswordChangeLockMinutes()
            : paySecurityProperties.getPasswordLockMinutes();
    }

    /** 作用域对应的锁定错误码，宿主按错误码渲染"余额支付/修改密码"各自的锁定文案。 */
    private static PaySecurityErrorCode lockedError(LockoutScopeEnum scope) {
        return scope == LockoutScopeEnum.PASSWORD_CHANGE
            ? PaySecurityErrorCode.PAY_PASSWORD_CHANGE_LOCKED
            : PaySecurityErrorCode.PAY_SECURITY_LOCKED;
    }
}
