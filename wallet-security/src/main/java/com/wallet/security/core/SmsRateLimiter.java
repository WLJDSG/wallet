package com.wallet.security.core;

import com.wallet.security.config.PaySecurityProperties;
import com.wallet.security.error.PaySecurityErrorCode;
import com.wallet.security.error.PaySecurityException;
import com.wallet.security.spi.PaySecurityKeyValueStore;

import java.time.ZoneId;

/**
 * 支付安全短信的发送频控与验证失败锁定。
 *
 * <p>发送侧：冷却占位 + 每日发送计数；验证侧：失败计数达到阈值时删除验证码并锁定，
 * 防止在验证码有效期内暴力尝试。</p>
 */
public final class SmsRateLimiter {

    private final PaySecurityKeyValueStore paySecurityKeyValueStore;
    private final RedisKeys redisKeys;
    private final PaySecurityProperties paySecurityProperties;
    private final ZoneId businessZone;

    public SmsRateLimiter(PaySecurityKeyValueStore paySecurityKeyValueStore, RedisKeys redisKeys, PaySecurityProperties paySecurityProperties,
        ZoneId businessZone) {
        this.paySecurityKeyValueStore = paySecurityKeyValueStore;
        this.redisKeys = redisKeys;
        this.paySecurityProperties = paySecurityProperties;
        this.businessZone = businessZone;
    }

    /**
     * 发送前占用冷却位并累加每日计数。
     *
     * @param purpose 验证码用途
     * @param phoneHash 手机号 SHA-256 摘要
     */
    public void ensureSendAllowed(String purpose, String phoneHash) {
        // setIfAbsent 即原子占位：占位成功即进入冷却，后续发送失败也不回滚，防止借失败绕过频控轰炸。
        String cooldownKey = redisKeys.smsSendCooldown(purpose, phoneHash);
        if (!paySecurityKeyValueStore.setIfAbsent(cooldownKey, "1", paySecurityProperties.getSmsCooldownSeconds())) {
            throw PaySecurityException.of(PaySecurityErrorCode.PAY_SMS_TOO_FREQUENT);
        }
        // 每日计数先加后判：超限的这次也计入当日额度，到业务时区次日零点自动清零。
        long dailyCount = paySecurityKeyValueStore.increment(redisKeys.smsSendDaily(purpose, phoneHash), 1L,
            BusinessTime.secondsUntilTomorrow(businessZone));
        if (dailyCount > paySecurityProperties.getSmsDailyLimit()) {
            throw PaySecurityException.of(PaySecurityErrorCode.PAY_SMS_DAILY_LIMIT);
        }
    }

    /**
     * 验证前检查锁定状态。
     *
     * @param purpose 验证码用途
     * @param phoneHash 手机号 SHA-256 摘要
     */
    public void ensureVerifyNotLocked(String purpose, String phoneHash) {
        if (paySecurityKeyValueStore.get(redisKeys.smsVerifyLock(purpose, phoneHash)) != null) {
            throw PaySecurityException.of(PaySecurityErrorCode.PAY_IDENTITY_CODE_LOCKED);
        }
    }

    /**
     * 记录一次验证失败；达到阈值时删除验证码并锁定验证。
     *
     * @param purpose 验证码用途
     * @param phoneHash 手机号 SHA-256 摘要
     * @param uid 用户ID（用于删除该用户的验证码）
     */
    public void recordVerifyFailure(String purpose, String phoneHash, Long uid) {
        long failures = paySecurityKeyValueStore.increment(redisKeys.smsVerifyFailure(purpose, phoneHash), 1L,
            paySecurityProperties.getSmsVerifyFailureWindowSeconds());
        // 达到失败上限：销毁当前验证码并锁定验证通道，阻断验证码有效期内的暴力尝试。
        if (failures >= paySecurityProperties.getSmsVerifyFailureLimit()) {
            paySecurityKeyValueStore.delete(redisKeys.smsCode(purpose, uid));
            paySecurityKeyValueStore.set(redisKeys.smsVerifyLock(purpose, phoneHash), "1", paySecurityProperties.getSmsVerifyLockSeconds());
        }
    }

    /**
     * 验证成功后清除失败计数。
     *
     * @param purpose 验证码用途
     * @param phoneHash 手机号 SHA-256 摘要
     */
    public void clearVerifyFailures(String purpose, String phoneHash) {
        paySecurityKeyValueStore.delete(redisKeys.smsVerifyFailure(purpose, phoneHash));
    }
}
