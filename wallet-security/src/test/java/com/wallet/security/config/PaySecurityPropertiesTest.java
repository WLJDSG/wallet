package com.wallet.security.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 默认阈值与既有生产基线逐项对齐（行为等价性核对清单）。
 */
public class PaySecurityPropertiesTest {

    @Test
    public void defaultsMatchLegacyProductionBaseline() {
        PaySecurityProperties properties = new PaySecurityProperties();
        assertTrue(properties.isEnabled());
        assertEquals(-1, properties.getEnforceVersion());
        assertEquals(300L, properties.getAuthorizationTtlSeconds());
        assertEquals(120L, properties.getChallengeTtlSeconds());
        assertEquals(5, properties.getPasswordContinuousFailureLimit());
        assertEquals(10, properties.getPasswordDailyFailureLimit());
        assertEquals(10, properties.getPasswordLockMinutes());
        assertEquals(30, properties.getPasswordChangeLockMinutes());
        assertEquals(60L, properties.getSmsCooldownSeconds());
        assertEquals(5, properties.getSmsDailyLimit());
        assertEquals(300L, properties.getSmsCodeTtlSeconds());
        assertEquals(5, properties.getSmsVerifyFailureLimit());
        assertEquals(600L, properties.getSmsVerifyFailureWindowSeconds());
        assertEquals(600L, properties.getSmsVerifyLockSeconds());
        assertEquals(12, properties.getBcryptStrength());
        assertEquals("Asia/Shanghai", properties.getBusinessZoneId());
        assertEquals("pay:", properties.getRedisKeyPrefix());
    }
}
