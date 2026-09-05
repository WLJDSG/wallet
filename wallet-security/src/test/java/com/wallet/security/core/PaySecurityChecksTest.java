package com.wallet.security.core;

import com.wallet.security.token.PayBiometricChallengeToken;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PaySecurityChecksTest {

    @Test
    public void passwordRuleRejectsOnlyInvalidFormatRepeatedAndStraightSequences() {
        assertFalse(PaySecurityChecks.isValidPassword(null));
        assertFalse(PaySecurityChecks.isValidPassword("12345"));
        assertFalse(PaySecurityChecks.isValidPassword("12a456"));
        assertFalse(PaySecurityChecks.isValidPassword("000000"));
        assertFalse(PaySecurityChecks.isValidPassword("123456"));
        assertFalse(PaySecurityChecks.isValidPassword("654321"));

        assertTrue(PaySecurityChecks.isValidPassword("121212"));
        assertFalse(PaySecurityChecks.isValidPassword("012345"));
        assertFalse(PaySecurityChecks.isValidPassword("987654"));
        assertTrue(PaySecurityChecks.isValidPassword("102345"));
        assertTrue(PaySecurityChecks.isValidPassword("135790"));
    }

    @Test
    public void enforceVersionUsesFixedServerBoundary() {
        assertFalse(PaySecurityChecks.shouldEnforce(-1, "100"));
        assertFalse(PaySecurityChecks.shouldEnforce(101, "100"));
        assertTrue(PaySecurityChecks.shouldEnforce(100, "100"));
        assertTrue(PaySecurityChecks.shouldEnforce(100, "101"));
    }

    @Test
    public void invalidClientVersionCannotDowngradeToLegacy() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
            () -> PaySecurityChecks.shouldEnforce(100, "unknown"));
    }

    @Test
    public void challengePayloadBindsEverySecurityFieldInStableOrder() {
        PayBiometricChallengeToken challenge = new PayBiometricChallengeToken();
        challenge.setNonce("nonce");
        challenge.setUid(7L);
        challenge.setCredentialId("credential");
        challenge.setOrderNo("order-1");
        challenge.setOrderType("order");
        challenge.setAmount(new BigDecimal("100.00"));
        challenge.setCurrency("JPY");
        challenge.setExpiresAt(123456789L);

        assertEquals("v1|challenge|nonce|7|credential|order-1|order|100.00|JPY|123456789",
            PaySecurityChecks.challengePayload("challenge", challenge));
    }

    @Test
    public void constantTimeComparisonHandlesNullAndExactValues() {
        assertTrue(PaySecurityChecks.constantTimeEquals("123456", "123456"));
        assertFalse(PaySecurityChecks.constantTimeEquals("123456", "123457"));
        assertFalse(PaySecurityChecks.constantTimeEquals(null, "123456"));
    }
}
