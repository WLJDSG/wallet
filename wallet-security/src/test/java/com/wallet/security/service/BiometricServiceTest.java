package com.wallet.security.service;

import com.wallet.security.testutil.PaySecurityEngineTestSupport;
import com.wallet.security.enums.AuthorizeMethodEnum;
import com.wallet.security.enums.BiometricPlatformEnum;
import com.wallet.security.enums.CredentialDisabledReasonEnum;
import com.wallet.security.enums.CredentialStatusEnum;
import com.wallet.security.enums.IdentityPurposeEnum;
import com.wallet.security.enums.SignAlgorithmEnum;
import com.wallet.security.error.PaySecurityErrorCode;
import com.wallet.security.model.AuthorizationResult;
import com.wallet.security.model.BiometricChallenge;
import com.wallet.security.model.BiometricCredentialInfo;
import com.wallet.security.model.BiometricRegistrationOptions;
import com.wallet.security.model.SecurityStatus;
import com.wallet.security.spi.model.BiometricCredential;
import com.wallet.security.spi.model.UserSecuritySettings;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BiometricServiceTest extends PaySecurityEngineTestSupport {

    private static final String PASSWORD = "739135";
    private KeyPair keyPair;

    private KeyPair newKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp256r1"));
        return generator.generateKeyPair();
    }

    private String publicKeyBase64(KeyPair pair) {
        return Base64.getEncoder().encodeToString(pair.getPublic().getEncoded());
    }

    private String sign(KeyPair pair, String payload) throws Exception {
        Signature signer = Signature.getInstance("SHA256withECDSA");
        signer.initSign(pair.getPrivate());
        signer.update(payload.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(signer.sign());
    }

    /** 完整开通流程：设密码 → 密码身份授权 → 注册会话 → 注册公钥凭证，返回凭证视图。 */
    private BiometricCredentialInfo enrollCredential() throws Exception {
        setPassword(PASSWORD);
        keyPair = newKeyPair();
        String enrollmentToken = engine.getIdentityService().authorizePasswordIdentity(user, IdentityPurposeEnum.BIOMETRIC_ENROLLMENT, PASSWORD,
            null, null, appClientInfo);
        BiometricRegistrationOptions options = engine.getBiometricService()
            .createRegistration(user, enrollmentToken, BiometricPlatformEnum.IOS);
        return engine.getBiometricService().registerCredential(user, options.getRegistrationId(), publicKeyBase64(keyPair),
            SignAlgorithmEnum.EC_P256_SHA256, sign(keyPair, options.getNonce()), appClientInfo);
    }

    @Test
    public void fullEnrollmentChallengeAndAuthorizationFlow() throws Exception {
        orders.register("o1", "order", new BigDecimal("100.00"), "JPY", false);
        BiometricCredentialInfo view = enrollCredential();
        assertNotNull(view.getCredentialId());
        assertEquals(CredentialStatusEnum.ENABLED, view.getStatus());
        assertEquals(BiometricPlatformEnum.IOS, view.getPlatform());
        assertTrue(view.getAvailable());

        SecurityStatus status = engine.getPasswordAuthService().getStatus(user, view.getCredentialId());
        assertTrue(status.getBiometricAvailable());
        assertEquals(AuthorizeMethodEnum.BIOMETRIC, status.getPreferredMethod());

        BiometricChallenge challenge = engine.getBiometricService()
            .createChallenge(user, view.getCredentialId(), "o1", "order");
        assertTrue(challenge.getSignPayload().startsWith("v1|" + challenge.getChallengeId() + "|"));
        assertTrue(challenge.getSignPayload().contains("|100.00|JPY|"));
        assertEquals(Long.valueOf(120L), challenge.getExpiresIn());

        AuthorizationResult result = engine.getBiometricService().authorize(user, challenge.getChallengeId(),
            view.getCredentialId(), sign(keyPair, challenge.getSignPayload()), appClientInfo);
        assertNotNull(result.getPayAuthorizationToken());
        assertTrue(audits.eventTypes().contains("BIOMETRIC_AUTHORIZE"));

        // 挑战一次性：重放同一挑战被拒绝
        expectError(PaySecurityErrorCode.BIOMETRIC_CHALLENGE_USED,
            () -> {
                try {
                    engine.getBiometricService().authorize(user, challenge.getChallengeId(), view.getCredentialId(),
                        sign(keyPair, challenge.getSignPayload()), appClientInfo);
                } catch (RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            });
    }

    @Test
    public void registrationIsIdempotentByRegistrationId() throws Exception {
        BiometricCredentialInfo first = enrollCredential();
        BiometricCredential stored = credentials.findByUidAndCredentialId(user.getUid(), first.getCredentialId());
        BiometricCredentialInfo replay = engine.getBiometricService().registerCredential(user, stored.getRegistrationId(),
            publicKeyBase64(keyPair), SignAlgorithmEnum.EC_P256_SHA256, "irrelevant", appClientInfo);
        assertEquals(first.getCredentialId(), replay.getCredentialId());
    }

    @Test
    public void enrollmentTokenIsSingleUse() throws Exception {
        setPassword(PASSWORD);
        keyPair = newKeyPair();
        String enrollmentToken = engine.getIdentityService().authorizePasswordIdentity(user, IdentityPurposeEnum.BIOMETRIC_ENROLLMENT, PASSWORD,
            null, null, appClientInfo);
        engine.getBiometricService().createRegistration(user, enrollmentToken, BiometricPlatformEnum.IOS);
        expectError(PaySecurityErrorCode.PAY_IDENTITY_TOKEN_INVALID,
            () -> engine.getBiometricService().createRegistration(user, enrollmentToken, BiometricPlatformEnum.IOS));
    }

    @Test
    public void enrollmentTokenBoundToPlatform() throws Exception {
        setPassword(PASSWORD);
        String enrollmentToken = engine.getIdentityService().authorizePasswordIdentity(user, IdentityPurposeEnum.BIOMETRIC_ENROLLMENT, PASSWORD,
            null, null, appClientInfo);
        expectError(PaySecurityErrorCode.PAY_IDENTITY_TOKEN_INVALID,
            () -> engine.getBiometricService().createRegistration(user, enrollmentToken, BiometricPlatformEnum.ANDROID));
    }

    @Test
    public void wrongAlgorithmAndTamperedSignatureRejected() throws Exception {
        setPassword(PASSWORD);
        keyPair = newKeyPair();
        String enrollmentToken = engine.getIdentityService().authorizePasswordIdentity(user, IdentityPurposeEnum.BIOMETRIC_ENROLLMENT, PASSWORD,
            null, null, appClientInfo);
        BiometricRegistrationOptions options = engine.getBiometricService()
            .createRegistration(user, enrollmentToken, BiometricPlatformEnum.IOS);
        expectError(PaySecurityErrorCode.BIOMETRIC_REGISTRATION_INVALID,
            () -> {
                try {
                    engine.getBiometricService().registerCredential(user, options.getRegistrationId(),
                        publicKeyBase64(keyPair), null, sign(keyPair, options.getNonce()), appClientInfo);
                } catch (RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            });
        expectError(PaySecurityErrorCode.BIOMETRIC_SIGNATURE_INVALID,
            () -> {
                try {
                    engine.getBiometricService().registerCredential(user, options.getRegistrationId(),
                        publicKeyBase64(keyPair), SignAlgorithmEnum.EC_P256_SHA256, sign(keyPair, "wrong-payload"), appClientInfo);
                } catch (RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            });
    }

    @Test
    public void enrollmentFromOrderRequiresPaidOrderAndRetriesAfterPayment() throws Exception {
        setPassword(PASSWORD);
        orders.register("o2", "order", new BigDecimal("50.00"), "JPY", false);
        String enrollmentToken = engine.getIdentityService().authorizePasswordIdentity(user, IdentityPurposeEnum.BIOMETRIC_ENROLLMENT, PASSWORD,
            "o2", "order", appClientInfo);
        expectError(PaySecurityErrorCode.BIOMETRIC_ENROLLMENT_ORDER_UNPAID,
            () -> engine.getBiometricService().createRegistration(user, enrollmentToken, BiometricPlatformEnum.IOS));
        // 未消费票据，支付完成后同一票据可继续开通
        orders.markPaid("o2");
        BiometricRegistrationOptions options = engine.getBiometricService()
            .createRegistration(user, enrollmentToken, BiometricPlatformEnum.IOS);
        assertNotNull(options.getNonce());
    }

    @Test
    public void revokeAllDisablesCredentialsAndBumpsSecurityVersion() throws Exception {
        BiometricCredentialInfo view = enrollCredential();
        engine.getBiometricService().revokeAll(user, appClientInfo);
        UserSecuritySettings profile = settings.findByUid(user.getUid());
        assertEquals(Integer.valueOf(3), profile.getSecurityVersion());
        BiometricCredential disabled = credentials.findByUidAndCredentialId(user.getUid(), view.getCredentialId());
        assertEquals(CredentialStatusEnum.DISABLED, disabled.getStatus());
        assertEquals(CredentialDisabledReasonEnum.USER_REVOKE_ALL, disabled.getDisabledReason());
        assertFalse(engine.getPasswordAuthService().getStatus(user, view.getCredentialId()).getBiometricAvailable());
        expectError(PaySecurityErrorCode.BIOMETRIC_CREDENTIAL_INVALID,
            () -> engine.getBiometricService().createChallenge(user, view.getCredentialId(), "o1", "order"));
    }

    @Test
    public void revokeSingleCredentialIsIdempotent() throws Exception {
        BiometricCredentialInfo view = enrollCredential();
        engine.getBiometricService().revokeCredential(user, view.getCredentialId(), appClientInfo);
        BiometricCredential revoked = credentials.findByUidAndCredentialId(user.getUid(), view.getCredentialId());
        assertEquals(CredentialStatusEnum.REVOKED, revoked.getStatus());
        assertEquals(CredentialDisabledReasonEnum.USER_UNBOUND, revoked.getDisabledReason());
        // 幂等：再次撤销静默返回
        engine.getBiometricService().revokeCredential(user, view.getCredentialId(), appClientInfo);
    }

    @Test
    public void authorizeOnlyTouchesLastUsedAtField() throws Exception {
        orders.register("o1", "order", new BigDecimal("100.00"), "JPY", false);
        BiometricCredentialInfo view = enrollCredential();
        BiometricCredential before = credentials.findByUidAndCredentialId(user.getUid(), view.getCredentialId());
        BiometricChallenge challenge = engine.getBiometricService()
            .createChallenge(user, view.getCredentialId(), "o1", "order");
        engine.getBiometricService().authorize(user, challenge.getChallengeId(), view.getCredentialId(),
            sign(keyPair, challenge.getSignPayload()), appClientInfo);
        BiometricCredential after = credentials.findByUidAndCredentialId(user.getUid(), view.getCredentialId());
        assertNotNull(after.getLastUsedAt());
        // 授权只允许写 last_used_at，其余字段必须保持原值（防回归到全量回写）
        assertEquals(before.getStatus(), after.getStatus());
        assertEquals(before.getPublicKey(), after.getPublicKey());
        assertEquals(before.getPasswordVersion(), after.getPasswordVersion());
        assertEquals(before.getSecurityVersion(), after.getSecurityVersion());
        assertEquals(before.getDisabledReason(), after.getDisabledReason());
    }

    @Test
    public void inFlightAuthorizeCannotResurrectRevokedCredential() throws Exception {
        orders.register("o1", "order", new BigDecimal("100.00"), "JPY", false);
        BiometricCredentialInfo view = enrollCredential();
        BiometricChallenge challenge = engine.getBiometricService()
            .createChallenge(user, view.getCredentialId(), "o1", "order");
        // 挑战创建后凭证被撤销：验签阶段拒绝，且凭证状态不被写回 ENABLED
        engine.getBiometricService().revokeCredential(user, view.getCredentialId(), appClientInfo);
        expectError(PaySecurityErrorCode.BIOMETRIC_CREDENTIAL_INVALID,
            () -> {
                try {
                    engine.getBiometricService().authorize(user, challenge.getChallengeId(), view.getCredentialId(),
                        sign(keyPair, challenge.getSignPayload()), appClientInfo);
                } catch (RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            });
        BiometricCredential stored = credentials.findByUidAndCredentialId(user.getUid(), view.getCredentialId());
        assertEquals(CredentialStatusEnum.REVOKED, stored.getStatus());
        assertEquals(CredentialDisabledReasonEnum.USER_UNBOUND, stored.getDisabledReason());
    }

    @Test
    public void authorizeReleasesChallengeWhenIssueFails() throws Exception {
        java.util.concurrent.atomic.AtomicBoolean failAuthWrite = new java.util.concurrent.atomic.AtomicBoolean();
        String authKeyPrefix = keys.authorization("");
        kv = new com.wallet.security.testutil.InMemoryKvStore() {
            @Override
            public boolean set(String key, String value, long ttlSeconds) {
                if (failAuthWrite.get() && key.startsWith(authKeyPrefix)) {
                    return false;
                }
                return super.set(key, value, ttlSeconds);
            }
        };
        rebuildEngine();
        orders.register("o1", "order", new BigDecimal("100.00"), "JPY", false);
        BiometricCredentialInfo view = enrollCredential();
        BiometricChallenge challenge = engine.getBiometricService()
            .createChallenge(user, view.getCredentialId(), "o1", "order");
        String signature = sign(keyPair, challenge.getSignPayload());
        // 占用挑战后签发票据失败：占用必须被释放，否则客户端没拿到票据挑战却已作废
        failAuthWrite.set(true);
        expectError(PaySecurityErrorCode.PAY_SECURITY_UNAVAILABLE,
            () -> engine.getBiometricService().authorize(user, challenge.getChallengeId(), view.getCredentialId(),
                signature, appClientInfo));
        assertFalse(kv.exists(keys.challengeUsed(challenge.getChallengeId())));
        // 同一挑战、同一签名在有效期内重试成功
        failAuthWrite.set(false);
        AuthorizationResult retry = engine.getBiometricService().authorize(user, challenge.getChallengeId(),
            view.getCredentialId(), signature, appClientInfo);
        assertNotNull(retry.getPayAuthorizationToken());
    }

    @Test
    public void concurrentProfileDriftDuringRevokeAllIsRejected() throws Exception {
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
        rejectNextCas.set(true);
        expectError(PaySecurityErrorCode.PAY_SECURITY_STATE_CHANGED,
            () -> engine.getBiometricService().revokeAll(user, appClientInfo));
        // CAS 拒绝后安全版本未被旧快照回写
        assertEquals(Integer.valueOf(2), settings.findByUid(user.getUid()).getSecurityVersion());
    }

    @Test
    public void credentialStatusUsesOwnedCredentialId() throws Exception {
        BiometricCredentialInfo view = enrollCredential();
        BiometricCredentialInfo status = engine.getBiometricService().getCredentialStatus(user, view.getCredentialId());
        assertTrue(status.getAvailable());
        expectError(PaySecurityErrorCode.BIOMETRIC_CREDENTIAL_INVALID,
            () -> engine.getBiometricService().getCredentialStatus(user, "missing"));
    }
}
