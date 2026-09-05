package com.wallet.security.service;

import com.wallet.security.testutil.PaySecurityEngineTestSupport;
import com.wallet.security.enums.BiometricPlatformEnum;
import com.wallet.security.enums.CredentialDisabledReasonEnum;
import com.wallet.security.enums.CredentialStatusEnum;
import com.wallet.security.enums.IdentityPurposeEnum;
import com.wallet.security.enums.LockoutScopeEnum;
import com.wallet.security.enums.PayPasswordStatusEnum;
import com.wallet.security.core.Hashes;
import com.wallet.security.error.PaySecurityErrorCode;
import com.wallet.security.spi.model.BiometricCredential;
import com.wallet.security.spi.model.UserSecuritySettings;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class IdentityServiceTest extends PaySecurityEngineTestSupport {

    private static final String PASSWORD = "739135";
    private static final String NEW_PASSWORD = "351397";

    @Test
    public void sendStoresGeneratedCodeAndDelegatesSendingOnly() {
        engine.getIdentityService().sendIdentityCode(user, IdentityPurposeEnum.PASSWORD_SET);
        assertEquals(user.getPhone(), sms.lastPhone);
        assertEquals(300L, sms.lastTtlSeconds);
        assertTrue(sms.lastCode.matches("^\\d{6}$"));
        assertEquals(sms.lastCode, kv.get(keys.smsCode("PASSWORD_SET", user.getUid())));
    }

    @Test
    public void purposeStateMachineGuardsSendAndAuthorize() {
        expectError(PaySecurityErrorCode.PAY_IDENTITY_PURPOSE_INVALID,
            () -> engine.getIdentityService().sendIdentityCode(user, IdentityPurposeEnum.PASSWORD_RESET));
        expectError(PaySecurityErrorCode.PAY_IDENTITY_PURPOSE_INVALID,
            () -> engine.getIdentityService().sendIdentityCode(user, null));
        setPassword(PASSWORD);
        expectError(PaySecurityErrorCode.PAY_IDENTITY_PURPOSE_INVALID,
            () -> engine.getIdentityService().sendIdentityCode(user, IdentityPurposeEnum.PASSWORD_SET));
        engine.getIdentityService().sendIdentityCode(user, IdentityPurposeEnum.PASSWORD_RESET);
        expectError(PaySecurityErrorCode.PAY_IDENTITY_PURPOSE_INVALID,
            () -> engine.getIdentityService().authorizePasswordIdentity(user, IdentityPurposeEnum.PASSWORD_RESET, PASSWORD, null, null, appClientInfo));
    }

    @Test
    public void cooldownBlocksImmediateResend() {
        engine.getIdentityService().sendIdentityCode(user, IdentityPurposeEnum.PASSWORD_SET);
        expectError(PaySecurityErrorCode.PAY_SMS_TOO_FREQUENT,
            () -> engine.getIdentityService().sendIdentityCode(user, IdentityPurposeEnum.PASSWORD_SET));
    }

    @Test
    public void dailySendLimitIsFive() {
        String phoneHash = phoneHash();
        for (int i = 0; i < 5; i++) {
            engine.getIdentityService().sendIdentityCode(user, IdentityPurposeEnum.PASSWORD_SET);
            kv.delete(keys.smsSendCooldown("PASSWORD_SET", phoneHash));
        }
        expectError(PaySecurityErrorCode.PAY_SMS_DAILY_LIMIT,
            () -> engine.getIdentityService().sendIdentityCode(user, IdentityPurposeEnum.PASSWORD_SET));
        assertEquals(5, sms.sendCount);
    }

    @Test
    public void sendFailureRollsBackStoredCode() {
        sms.result = false;
        expectError(PaySecurityErrorCode.PAY_SECURITY_UNAVAILABLE,
            () -> engine.getIdentityService().sendIdentityCode(user, IdentityPurposeEnum.PASSWORD_SET));
        assertFalse(kv.exists(keys.smsCode("PASSWORD_SET", user.getUid())));
    }

    @Test
    public void hostSendExceptionPropagatesAndRollsBackStoredCode() {
        sms.throwOnSend = new IllegalStateException("sms margin insufficient");
        try {
            engine.getIdentityService().sendIdentityCode(user, IdentityPurposeEnum.PASSWORD_SET);
        } catch (IllegalStateException expected) {
            assertFalse(kv.exists(keys.smsCode("PASSWORD_SET", user.getUid())));
            return;
        }
        throw new AssertionError("宿主短信异常应原样穿透");
    }

    @Test
    public void verifyFailuresLockAfterFiveAndDeleteCode() {
        engine.getIdentityService().sendIdentityCode(user, IdentityPurposeEnum.PASSWORD_SET);
        for (int i = 0; i < 5; i++) {
            expectError(PaySecurityErrorCode.PAY_IDENTITY_CODE_INVALID,
                () -> engine.getIdentityService().verifyIdentityCode(user, IdentityPurposeEnum.PASSWORD_SET, "000001", appClientInfo));
        }
        assertFalse(kv.exists(keys.smsCode("PASSWORD_SET", user.getUid())));
        expectError(PaySecurityErrorCode.PAY_IDENTITY_CODE_LOCKED,
            () -> engine.getIdentityService().verifyIdentityCode(user, IdentityPurposeEnum.PASSWORD_SET, "000001", appClientInfo));
    }

    @Test
    public void setPasswordFlowInitializesProfileVersions() {
        setPassword(PASSWORD);
        UserSecuritySettings profile = settings.findByUid(user.getUid());
        assertNotNull(profile);
        assertEquals(Integer.valueOf(1), profile.getPasswordVersion());
        // 首次设置递增到 2，使未设密码状态签发的 LEGACY_CONFIRM 票据立即失效
        assertEquals(Integer.valueOf(2), profile.getSecurityVersion());
        assertEquals(PayPasswordStatusEnum.ENABLED, profile.getPasswordStatus());
        assertNotNull(profile.getPasswordSetAt());
        assertNotNull(profile.getPasswordHash());
        assertTrue(audits.eventTypes().contains("PASSWORD_SET"));
    }

    @Test
    public void weakOrMismatchedPasswordsRejectedBeforeTokenChecks() {
        expectError(PaySecurityErrorCode.PAY_PASSWORD_INVALID,
            () -> engine.getIdentityService().updatePassword(user, IdentityPurposeEnum.PASSWORD_SET, "any", "123456", "123456", appClientInfo));
        expectError(PaySecurityErrorCode.PAY_PASSWORD_INVALID,
            () -> engine.getIdentityService().updatePassword(user, IdentityPurposeEnum.PASSWORD_SET, "any", PASSWORD, NEW_PASSWORD, appClientInfo));
    }

    @Test
    public void identityTokenIsSingleUse() {
        engine.getIdentityService().sendIdentityCode(user, IdentityPurposeEnum.PASSWORD_SET);
        String token = engine.getIdentityService().verifyIdentityCode(user, IdentityPurposeEnum.PASSWORD_SET, sms.lastCode, appClientInfo);
        // 模拟并发占用：used 标记已存在但票据仍在
        kv.set(keys.identityUsed(Hashes.sha256(token)), "1", 300L);
        expectError(PaySecurityErrorCode.PAY_IDENTITY_TOKEN_USED,
            () -> engine.getIdentityService().updatePassword(user, IdentityPurposeEnum.PASSWORD_SET, token, PASSWORD, PASSWORD, appClientInfo));
        // 消费成功后重放：票据已删除
        kv.delete(keys.identityUsed(Hashes.sha256(token)));
        engine.getIdentityService().updatePassword(user, IdentityPurposeEnum.PASSWORD_SET, token, PASSWORD, PASSWORD, appClientInfo);
        expectError(PaySecurityErrorCode.PAY_IDENTITY_TOKEN_INVALID,
            () -> engine.getIdentityService().updatePassword(user, IdentityPurposeEnum.PASSWORD_SET, token, PASSWORD, PASSWORD, appClientInfo));
    }

    @Test
    public void securityVersionDriftInvalidatesIdentityToken() {
        setPassword(PASSWORD);
        String token = engine.getIdentityService().authorizePasswordIdentity(user, IdentityPurposeEnum.PASSWORD_CHANGE, PASSWORD, null, null,
            appClientInfo);
        // 全量撤销凭证会递增 securityVersion，使已签发的身份票据失效
        engine.getBiometricService().revokeAll(user, appClientInfo);
        expectError(PaySecurityErrorCode.PAY_SECURITY_STATE_CHANGED,
            () -> engine.getIdentityService().updatePassword(user, IdentityPurposeEnum.PASSWORD_CHANGE, token, NEW_PASSWORD, NEW_PASSWORD,
                appClientInfo));
    }

    @Test
    public void changePasswordBumpsVersionsAndDisablesCredentials() {
        setPassword(PASSWORD);
        BiometricCredential credential = new BiometricCredential();
        credential.setCredentialId("cred-1");
        credential.setRegistrationId("reg-1");
        credential.setUid(user.getUid());
        credential.setPlatform(BiometricPlatformEnum.IOS);
        credential.setPasswordVersion(1);
        credential.setSecurityVersion(2);
        credential.setStatus(CredentialStatusEnum.ENABLED);
        credentials.insert(credential);

        String token = engine.getIdentityService().authorizePasswordIdentity(user, IdentityPurposeEnum.PASSWORD_CHANGE, PASSWORD, null, null,
            appClientInfo);
        engine.getIdentityService().updatePassword(user, IdentityPurposeEnum.PASSWORD_CHANGE, token, NEW_PASSWORD, NEW_PASSWORD, appClientInfo);

        UserSecuritySettings profile = settings.findByUid(user.getUid());
        assertEquals(Integer.valueOf(2), profile.getPasswordVersion());
        assertEquals(Integer.valueOf(3), profile.getSecurityVersion());
        BiometricCredential disabled = credentials.findByUidAndCredentialId(user.getUid(), "cred-1");
        assertEquals(CredentialStatusEnum.DISABLED, disabled.getStatus());
        assertEquals(CredentialDisabledReasonEnum.PASSWORD_CHANGED, disabled.getDisabledReason());
        assertTrue(audits.eventTypes().contains("PASSWORD_CHANGE"));
    }

    @Test
    public void concurrentProfileDriftDuringPasswordChangeIsRejected() {
        // 拒绝一次 CAS，模拟读取档案与写入之间发生并发修改（另一次改密/全量撤销）
        java.util.concurrent.atomic.AtomicBoolean rejectNextCas = new java.util.concurrent.atomic.AtomicBoolean();
        settings = new com.wallet.security.testutil.InMemoryUserSecuritySettingsRepository() {
            @Override
            public synchronized boolean updateWithVersion(UserSecuritySettings profile, int expectedSecurityVersion) {
                if (rejectNextCas.getAndSet(false)) {
                    return false;
                }
                return super.updateWithVersion(profile, expectedSecurityVersion);
            }
        };
        rebuildEngine();
        setPassword(PASSWORD);
        String token = engine.getIdentityService().authorizePasswordIdentity(user, IdentityPurposeEnum.PASSWORD_CHANGE,
            PASSWORD, null, null, appClientInfo);
        rejectNextCas.set(true);
        expectError(PaySecurityErrorCode.PAY_SECURITY_STATE_CHANGED,
            () -> engine.getIdentityService().updatePassword(user, IdentityPurposeEnum.PASSWORD_CHANGE, token, NEW_PASSWORD,
                NEW_PASSWORD, appClientInfo));
        // CAS 拒绝后档案未被写入：密码版本与安全版本保持首次设置后的取值
        UserSecuritySettings profile = settings.findByUid(user.getUid());
        assertEquals(Integer.valueOf(1), profile.getPasswordVersion());
        assertEquals(Integer.valueOf(2), profile.getSecurityVersion());
    }

    @Test
    public void wrongCurrentPasswordOnIdentityAuthorizeCountsFailures() {
        setPassword(PASSWORD);
        expectError(PaySecurityErrorCode.PAY_PASSWORD_ERROR,
            () -> engine.getIdentityService().authorizePasswordIdentity(user, IdentityPurposeEnum.PASSWORD_CHANGE, "111112", null, null,
                appClientInfo));
        // 失败计入修改密码作用域的独立计数，不写余额支付作用域
        assertTrue(kv.exists(keys.continuousFailure(LockoutScopeEnum.PASSWORD_CHANGE, user.getUid())));
        assertFalse(kv.exists(keys.continuousFailure(LockoutScopeEnum.BALANCE_PAY, user.getUid())));
        // 密码身份验证失败必须留审计痕迹
        assertTrue(audits.eventTypes().contains("PASSWORD_IDENTITY_AUTHORIZE"));
    }

    @Test
    public void fiveContinuousIdentityFailuresLockPasswordChangeForThirtyMinutesOnly() {
        setPassword(PASSWORD);
        orders.register("o1", "order", new java.math.BigDecimal("100.00"), "JPY", false);
        for (int i = 1; i <= 4; i++) {
            expectError(PaySecurityErrorCode.PAY_PASSWORD_ERROR,
                () -> engine.getIdentityService().authorizePasswordIdentity(user, IdentityPurposeEnum.PASSWORD_CHANGE,
                    "111112", null, null, appClientInfo));
        }
        // 第 5 次连续失败触发修改密码功能锁定
        expectError(PaySecurityErrorCode.PAY_PASSWORD_CHANGE_LOCKED,
            () -> engine.getIdentityService().authorizePasswordIdentity(user, IdentityPurposeEnum.PASSWORD_CHANGE,
                "111112", null, null, appClientInfo));
        // 锁定后即使密码正确也拒绝
        expectError(PaySecurityErrorCode.PAY_PASSWORD_CHANGE_LOCKED,
            () -> engine.getIdentityService().authorizePasswordIdentity(user, IdentityPurposeEnum.PASSWORD_CHANGE,
                PASSWORD, null, null, appClientInfo));
        // 锁定时长为修改密码作用域的 30 分钟（区别于余额支付的 10 分钟）
        long lockedUntil = Long.parseLong(kv.get(keys.lock(LockoutScopeEnum.PASSWORD_CHANGE, user.getUid())));
        long expectedLockMillis = lockedUntil - System.currentTimeMillis();
        assertTrue(expectedLockMillis > 29 * 60 * 1000L && expectedLockMillis <= 30 * 60 * 1000L);
        // 修改密码锁定不影响余额支付：支付密码授权仍然可用
        assertNotNull(engine.getPasswordAuthService()
            .authorizePassword(user, "o1", "order", PASSWORD, appClientInfo).getPayAuthorizationToken());
        assertFalse(engine.getPasswordAuthService().getStatus(user, null).getBalancePayLocked());
        // 短信重置密码是被锁定用户的自救通道：重置成功即解除修改密码锁定
        engine.getIdentityService().sendIdentityCode(user, IdentityPurposeEnum.PASSWORD_RESET);
        String resetToken = engine.getIdentityService().verifyIdentityCode(user, IdentityPurposeEnum.PASSWORD_RESET,
            sms.lastCode, appClientInfo);
        engine.getIdentityService().updatePassword(user, IdentityPurposeEnum.PASSWORD_RESET, resetToken, NEW_PASSWORD,
            NEW_PASSWORD, appClientInfo);
        assertFalse(kv.exists(keys.lock(LockoutScopeEnum.PASSWORD_CHANGE, user.getUid())));
        assertNotNull(engine.getIdentityService().authorizePasswordIdentity(user, IdentityPurposeEnum.PASSWORD_CHANGE,
            NEW_PASSWORD, null, null, appClientInfo));
    }

    @Test
    public void verifyCodeConsumptionIsAtomicOnDelete() {
        engine.getIdentityService().sendIdentityCode(user, IdentityPurposeEnum.PASSWORD_SET);
        String code = sms.lastCode;
        engine.getIdentityService().verifyIdentityCode(user, IdentityPurposeEnum.PASSWORD_SET, code, appClientInfo);
        assertTrue(audits.eventTypes().contains("SMS_CODE_VERIFY"));
        // 一码一用：验证成功即销毁，重放同一验证码按失效处理
        expectError(PaySecurityErrorCode.PAY_IDENTITY_CODE_INVALID,
            () -> engine.getIdentityService().verifyIdentityCode(user, IdentityPurposeEnum.PASSWORD_SET, code, appClientInfo));
    }

    @Test
    public void inactiveUserRejectedAtEveryIdentityEntry() {
        com.wallet.security.model.UserIdentity inactive =
            com.wallet.security.model.UserIdentity.of(user.getUid(), user.getPhone(), false);
        expectError(PaySecurityErrorCode.PAY_SECURITY_USER_INVALID,
            () -> engine.getIdentityService().sendIdentityCode(inactive, IdentityPurposeEnum.PASSWORD_SET));
        expectError(PaySecurityErrorCode.PAY_SECURITY_USER_INVALID,
            () -> engine.getIdentityService().verifyIdentityCode(inactive, IdentityPurposeEnum.PASSWORD_SET, "123456", appClientInfo));
        expectError(PaySecurityErrorCode.PAY_SECURITY_USER_INVALID,
            () -> engine.getIdentityService().updatePassword(inactive, IdentityPurposeEnum.PASSWORD_SET, "any", PASSWORD,
                PASSWORD, appClientInfo));
    }
}
