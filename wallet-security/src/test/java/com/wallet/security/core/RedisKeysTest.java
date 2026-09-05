package com.wallet.security.core;

import com.wallet.security.enums.LockoutScopeEnum;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 键模板与既有生产键逐字面量对齐；任何断言失败都意味着存储协议被破坏。
 */
public class RedisKeysTest {

    private final ZoneId zone = ZoneId.of("Asia/Shanghai");
    private final RedisKeys keys = new RedisKeys("pay:", zone);

    @Test
    public void allKeyTemplatesMatchLegacyLiterals() {
        String today = LocalDate.now(zone).toString();
        assertEquals("pay:auth:h1", keys.authorization("h1"));
        assertEquals("pay:auth:used:h1", keys.authorizationUsed("h1"));
        // 余额支付作用域沿用既有生产键（无中缀）
        assertEquals("pay:pwd:fail:continuous:7", keys.continuousFailure(LockoutScopeEnum.BALANCE_PAY, 7L));
        assertEquals("pay:pwd:fail:daily:" + today + ":7", keys.dailyFailure(LockoutScopeEnum.BALANCE_PAY, 7L));
        assertEquals("pay:pwd:lock:7", keys.lock(LockoutScopeEnum.BALANCE_PAY, 7L));
        // 修改支付密码作用域使用独立键，与余额支付互不影响
        assertEquals("pay:pwd:change:fail:continuous:7", keys.continuousFailure(LockoutScopeEnum.PASSWORD_CHANGE, 7L));
        assertEquals("pay:pwd:change:fail:daily:" + today + ":7", keys.dailyFailure(LockoutScopeEnum.PASSWORD_CHANGE, 7L));
        assertEquals("pay:pwd:change:lock:7", keys.lock(LockoutScopeEnum.PASSWORD_CHANGE, 7L));
        assertEquals("pay:identity:h1", keys.identity("h1"));
        assertEquals("pay:identity:used:h1", keys.identityUsed("h1"));
        assertEquals("pay:identity:BIOMETRIC_ENROLLMENT:h1", keys.enrollment("h1"));
        assertEquals("pay:identity:BIOMETRIC_ENROLLMENT:used:h1", keys.enrollmentUsed("h1"));
        assertEquals("pay:bio:registration:r1", keys.registration("r1"));
        assertEquals("pay:bio:registration:used:r1", keys.registrationUsed("r1"));
        assertEquals("pay:bio:challenge:c1", keys.challenge("c1"));
        assertEquals("pay:bio:challenge:used:c1", keys.challengeUsed("c1"));
        assertEquals("pay:sms:PASSWORD_SET:7", keys.smsCode("PASSWORD_SET", 7L));
        assertEquals("pay:sms:send:cooldown:PASSWORD_SET:ph", keys.smsSendCooldown("PASSWORD_SET", "ph"));
        assertEquals("pay:sms:send:daily:" + today + ":PASSWORD_SET:ph",
            keys.smsSendDaily("PASSWORD_SET", "ph"));
        assertEquals("pay:sms:verify:fail:PASSWORD_SET:ph", keys.smsVerifyFailure("PASSWORD_SET", "ph"));
        assertEquals("pay:sms:verify:lock:PASSWORD_SET:ph", keys.smsVerifyLock("PASSWORD_SET", "ph"));
    }
}
