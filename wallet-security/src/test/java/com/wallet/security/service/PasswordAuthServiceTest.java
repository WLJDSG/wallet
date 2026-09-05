package com.wallet.security.service;

import com.wallet.security.testutil.PaySecurityEngineTestSupport;
import com.wallet.security.core.BusinessTime;
import com.wallet.security.enums.AuthorizeMethodEnum;
import com.wallet.security.enums.IdentityPurposeEnum;
import com.wallet.security.enums.LockoutScopeEnum;
import com.wallet.security.error.PaySecurityErrorCode;
import com.wallet.security.error.PaySecurityException;
import com.wallet.security.model.AuthorizationResult;
import com.wallet.security.model.SecurityStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PasswordAuthServiceTest extends PaySecurityEngineTestSupport {

    private static final String PASSWORD = "739135";

    @Test
    public void statusForFreshUserPrefersLegacyConfirm() {
        SecurityStatus status = engine.getPasswordAuthService().getStatus(user, null);
        assertFalse(status.getPasswordSet());
        assertFalse(status.getBalancePayLocked());
        assertNull(status.getLockedUntil());
        assertFalse(status.getBiometricAvailable());
        assertEquals(AuthorizeMethodEnum.LEGACY_CONFIRM, status.getPreferredMethod());
        assertEquals("v1", status.getSecurityPolicyVersion());
    }

    @Test
    public void authorizePasswordRequiresPasswordSet() {
        expectError(PaySecurityErrorCode.PAY_PASSWORD_NOT_SET,
            () -> engine.getPasswordAuthService().authorizePassword(user, "o1", "order", PASSWORD, appClientInfo));
    }

    @Test
    public void wrongPasswordCountsRemainingAttemptsAndLocksAfterFiveContinuousFailures() {
        setPassword(PASSWORD);
        orders.register("o1", "order", new BigDecimal("100.00"), "JPY", false);
        for (int i = 1; i <= 4; i++) {
            PaySecurityException error = expectError(PaySecurityErrorCode.PAY_PASSWORD_ERROR,
                () -> engine.getPasswordAuthService().authorizePassword(user, "o1", "order", "111112", appClientInfo));
            assertEquals(5 - i, error.getData());
        }
        expectError(PaySecurityErrorCode.PAY_SECURITY_LOCKED,
            () -> engine.getPasswordAuthService().authorizePassword(user, "o1", "order", "111112", appClientInfo));
        SecurityStatus status = engine.getPasswordAuthService().getStatus(user, null);
        assertTrue(status.getBalancePayLocked());
        assertNotNull(status.getLockedUntil());
        assertTrue(status.getLockedUntil().getTime() > System.currentTimeMillis());
        // 锁定后即使密码正确也拒绝授权
        expectError(PaySecurityErrorCode.PAY_SECURITY_LOCKED,
            () -> engine.getPasswordAuthService().authorizePassword(user, "o1", "order", PASSWORD, appClientInfo));
        // 余额支付锁定只作用于支付通道：修改密码前的身份校验仍可正常进行
        assertNotNull(engine.getIdentityService().authorizePasswordIdentity(user, IdentityPurposeEnum.PASSWORD_CHANGE,
            PASSWORD, null, null, appClientInfo));
        assertFalse(kv.exists(keys.lock(LockoutScopeEnum.PASSWORD_CHANGE, user.getUid())));
    }

    @Test
    public void dailyFailureLimitLocksUntilStartOfTomorrow() {
        setPassword(PASSWORD);
        orders.register("o1", "order", new BigDecimal("100.00"), "JPY", false);
        // 4 失败 + 成功（清连续）× 2 轮 → 当日累计 8，再失败 2 次触发当日上限
        for (int round = 0; round < 2; round++) {
            for (int i = 0; i < 4; i++) {
                expectError(PaySecurityErrorCode.PAY_PASSWORD_ERROR,
                    () -> engine.getPasswordAuthService().authorizePassword(user, "o1", "order", "111112", appClientInfo));
            }
            engine.getPasswordAuthService().authorizePassword(user, "o1", "order", PASSWORD, appClientInfo);
        }
        PaySecurityException ninth = expectError(PaySecurityErrorCode.PAY_PASSWORD_ERROR,
            () -> engine.getPasswordAuthService().authorizePassword(user, "o1", "order", "111112", appClientInfo));
        assertEquals(1, ninth.getData());
        long expectedBefore = BusinessTime.startOfTomorrowMillis(ZoneId.of(properties.getBusinessZoneId()));
        expectError(PaySecurityErrorCode.PAY_SECURITY_LOCKED,
            () -> engine.getPasswordAuthService().authorizePassword(user, "o1", "order", "111112", appClientInfo));
        long expectedAfter = BusinessTime.startOfTomorrowMillis(ZoneId.of(properties.getBusinessZoneId()));
        long lockedUntil = engine.getPasswordAuthService().getStatus(user, null).getLockedUntil().getTime();
        assertTrue(lockedUntil == expectedBefore || lockedUntil == expectedAfter);
    }

    @Test
    public void successfulAuthorizeIssuesTokenClearsContinuousFailuresAndOffersEnrollment() {
        setPassword(PASSWORD);
        orders.register("o1", "order", new BigDecimal("100.00"), "JPY", false);
        expectError(PaySecurityErrorCode.PAY_PASSWORD_ERROR,
            () -> engine.getPasswordAuthService().authorizePassword(user, "o1", "order", "111112", appClientInfo));
        AuthorizationResult result = engine.getPasswordAuthService()
            .authorizePassword(user, "o1", "order", PASSWORD, appClientInfo);
        assertNotNull(result.getPayAuthorizationToken());
        assertEquals(Long.valueOf(300L), result.getExpiresIn());
        // app 渠道顺带签发生物注册票据
        assertNotNull(result.getBiometricEnrollmentToken());
        assertFalse(kv.exists(keys.continuousFailure(LockoutScopeEnum.BALANCE_PAY, user.getUid())));
        assertTrue(audits.eventTypes().contains("PAY_AUTH_ISSUE"));
        assertTrue(audits.eventTypes().contains("PASSWORD_AUTHORIZE"));
    }

    @Test
    public void nonAppChannelDoesNotIssueEnrollmentToken() {
        setPassword(PASSWORD);
        orders.register("o1", "order", new BigDecimal("100.00"), "JPY", false);
        appClientInfo.setPlatform("h5");
        AuthorizationResult result = engine.getPasswordAuthService()
            .authorizePassword(user, "o1", "order", PASSWORD, appClientInfo);
        assertNull(result.getBiometricEnrollmentToken());
    }

    @Test
    public void paidOrderCannotBeAuthorized() {
        setPassword(PASSWORD);
        orders.register("o1", "order", new BigDecimal("100.00"), "JPY", true);
        expectError(PaySecurityErrorCode.ORDER_PAID,
            () -> engine.getPasswordAuthService().authorizePassword(user, "o1", "order", PASSWORD, appClientInfo));
    }

    @Test
    public void legacyConfirmOnlyForUsersWithoutPassword() {
        orders.register("o1", "order", new BigDecimal("100.00"), "JPY", false);
        AuthorizationResult result = engine.getPasswordAuthService()
            .authorizeLegacyConfirm(user, "o1", "order", appClientInfo);
        assertNotNull(result.getPayAuthorizationToken());
        assertTrue(audits.eventTypes().contains("LEGACY_CONFIRM_AUTHORIZE"));
        setPassword(PASSWORD);
        expectError(PaySecurityErrorCode.PAY_PASSWORD_ALREADY_SET,
            () -> engine.getPasswordAuthService().authorizeLegacyConfirm(user, "o1", "order", appClientInfo));
    }
}
