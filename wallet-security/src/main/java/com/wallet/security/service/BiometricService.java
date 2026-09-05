package com.wallet.security.service;

import com.wallet.security.core.Hashes;
import com.wallet.security.core.PaySecurityChecks;
import com.wallet.security.core.Texts;
import com.wallet.security.enums.AttestationStatusEnum;
import com.wallet.security.enums.AuditEventEnum;
import com.wallet.security.enums.AuditResultEnum;
import com.wallet.security.enums.AuthorizeMethodEnum;
import com.wallet.security.enums.BiometricPlatformEnum;
import com.wallet.security.enums.CredentialDisabledReasonEnum;
import com.wallet.security.enums.CredentialStatusEnum;
import com.wallet.security.enums.LockoutScopeEnum;
import com.wallet.security.enums.PayPasswordStatusEnum;
import com.wallet.security.enums.SignAlgorithmEnum;
import com.wallet.security.error.PaySecurityErrorCode;
import com.wallet.security.error.PaySecurityException;
import com.wallet.security.model.AuthorizationResult;
import com.wallet.security.model.BiometricChallenge;
import com.wallet.security.model.BiometricCredentialInfo;
import com.wallet.security.model.BiometricRegistrationOptions;
import com.wallet.security.model.ClientInfo;
import com.wallet.security.model.OrderPaymentInfo;
import com.wallet.security.model.UserIdentity;
import com.wallet.security.spi.model.BiometricCredential;
import com.wallet.security.spi.model.UserSecuritySettings;
import com.wallet.security.token.PayBiometricChallengeToken;
import com.wallet.security.token.PayBiometricRegistrationToken;

import java.math.BigDecimal;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 生物识别支付：凭证注册、挑战验签、凭证生命周期管理。
 *
 * <p>设备侧密钥对（Android Keystore / iOS Secure Enclave 持私钥），服务端只存
 * X.509 P-256 公钥；注册签名只证明客户端持有上传公钥对应的私钥。</p>
 *
 * <p>注册（开通）三步：密码校验签发注册票据 → {@link #createRegistration} 消费票据换取
 * 注册会话与签名随机数 → {@link #registerCredential} 验签随机数证明持有私钥并落库凭证。
 * 支付两步：{@link #createChallenge} 生成订单绑定挑战 → {@link #authorize} 验签换取支付授权票据。</p>
 */
public final class BiometricService {

    private final PaySecurityRuntime paySecurityRuntime;

    /** 内部构造器，宿主请通过 {@code PaySecurityEngine} 装配。 */
    public BiometricService(PaySecurityRuntime paySecurityRuntime) {
        this.paySecurityRuntime = paySecurityRuntime;
    }

    /**
     * 消费生物注册授权票据并创建一次性注册会话。
     *
     * <p>流程节点：注册票据与 uid/平台绑定核对 → 锁定检查 → 密码与安全版本未漂移核对
     * → 来源订单已支付复核 → 原子占用注册票据 → 生成注册会话与服务端随机数
     * → 会话落存储失败回滚占用 → 销毁原票据。</p>
     *
     * @param user 当前登录用户
     * @param biometricEnrollmentToken 支付密码校验后签发的一次性生物注册票据
     * @param platform 原生平台：ANDROID或IOS
     * @return 注册会话和签名随机数
     */
    public BiometricRegistrationOptions createRegistration(UserIdentity user, String biometricEnrollmentToken,
        BiometricPlatformEnum platform) {
        paySecurityRuntime.ensureActiveUser(user);
        String enrollmentHash = Hashes.sha256(biometricEnrollmentToken);
        String enrollmentKey = paySecurityRuntime.getRedisKeys().enrollment(enrollmentHash);
        PayBiometricRegistrationToken enrollment = paySecurityRuntime.getJsonHelper()
            .fromJson(paySecurityRuntime.getPaySecurityKeyValueStore().get(enrollmentKey), PayBiometricRegistrationToken.class);
        // 注册票据与用户、平台绑定，任一不匹配即拒绝；私钥持有证明在凭证注册步骤完成。
        if (enrollment == null || !user.getUid().equals(enrollment.getUid())
            || platform != enrollment.getPlatform()) {
            throw PaySecurityException.of(PaySecurityErrorCode.PAY_IDENTITY_TOKEN_INVALID);
        }
        paySecurityRuntime.getLockoutManager().ensureNotLocked(LockoutScopeEnum.BALANCE_PAY, user.getUid());
        // 票据签发后密码或安全版本已变化（改密、撤销凭证等）时，未完成的开通流程必须作废。
        UserSecuritySettings security = paySecurityRuntime.getSettings(user.getUid());
        if (security == null || security.getPasswordStatus() != PayPasswordStatusEnum.ENABLED
            || !security.getSecurityVersion().equals(enrollment.getSecurityVersion())
            || !security.getPasswordVersion().equals(enrollment.getPasswordVersion())) {
            throw PaySecurityException.of(PaySecurityErrorCode.PAY_SECURITY_STATE_CHANGED);
        }
        if (Texts.isNotBlank(enrollment.getOrderNo())) {
            // 支付成功页免二次输密开通时，必须重新确认来源订单已经支付成功。
            OrderPaymentInfo paidOrder = paySecurityRuntime.resolveOrder(user.getUid(), enrollment.getOrderNo(),
                enrollment.getOrderType());
            if (!paidOrder.getPaid()) {
                throw PaySecurityException.of(PaySecurityErrorCode.BIOMETRIC_ENROLLMENT_ORDER_UNPAID);
            }
        }
        // 先原子占用注册授权票据，再创建注册会话，防止同一票据并发生成多个会话。
        String enrollmentUsedKey = paySecurityRuntime.getRedisKeys().enrollmentUsed(enrollmentHash);
        if (!paySecurityRuntime.getPaySecurityKeyValueStore().setIfAbsent(enrollmentUsedKey, "1",
            paySecurityRuntime.authorizationTtlSeconds())) {
            throw PaySecurityException.of(PaySecurityErrorCode.PAY_IDENTITY_TOKEN_USED);
        }
        String registrationId = UUID.randomUUID().toString();
        enrollment.setNonce(paySecurityRuntime.randomToken());
        enrollment.setEnrollmentTokenHash(enrollmentHash);
        String registrationKey = paySecurityRuntime.getRedisKeys().registration(registrationId);
        String registrationJson = paySecurityRuntime.getJsonHelper().toJson(enrollment);
        if (!paySecurityRuntime.getPaySecurityKeyValueStore().set(registrationKey, registrationJson,
            paySecurityRuntime.authorizationTtlSeconds())) {
            paySecurityRuntime.getPaySecurityKeyValueStore().delete(enrollmentUsedKey);
            throw PaySecurityException.of(PaySecurityErrorCode.PAY_SECURITY_UNAVAILABLE);
        }
        paySecurityRuntime.getPaySecurityKeyValueStore().delete(enrollmentKey);
        BiometricRegistrationOptions options = new BiometricRegistrationOptions();
        options.setRegistrationId(registrationId);
        options.setNonce(enrollment.getNonce());
        options.setExpiresIn(paySecurityRuntime.authorizationTtlSeconds());
        return options;
    }

    /**
     * 验证注册签名并幂等创建设备公钥凭证。
     *
     * <p>流程节点：registrationId 幂等命中直接返回 → 会话归属与算法白名单校验
     * → 密码与安全版本未漂移核对 → 严格解析 P-256 公钥 → 验签服务端随机数（私钥持有证明）
     * → 原子占用会话（并发时二次幂等查询）→ 凭证落库 → 审计。</p>
     *
     * @param user 当前登录用户
     * @param registrationId 服务端签发的一次性注册会话ID（持久化唯一索引保证幂等）
     * @param publicKey Base64编码的X.509 P-256公钥
     * @param algorithm 签名算法，固定为EC_P256_SHA256
     * @param signature 对注册随机数生成的Base64 DER ECDSA签名
     * @param clientInfo 客户端环境快照
     * @return 已创建或幂等命中的生物支付凭证
     */
    public BiometricCredentialInfo registerCredential(UserIdentity user, String registrationId, String publicKey,
        SignAlgorithmEnum algorithm, String signature, ClientInfo clientInfo) {
        paySecurityRuntime.ensureActiveUser(user);
        // registrationId 持久化并建立唯一索引，成功后的重复请求直接返回原 Credential。
        BiometricCredential existing = paySecurityRuntime.getBiometricCredentialRepository().findByUidAndRegistrationId(user.getUid(), registrationId);
        if (existing != null) {
            return paySecurityRuntime.credentialView(existing);
        }
        String registrationKey = paySecurityRuntime.getRedisKeys().registration(registrationId);
        PayBiometricRegistrationToken registration = paySecurityRuntime.getJsonHelper()
            .fromJson(paySecurityRuntime.getPaySecurityKeyValueStore().get(registrationKey), PayBiometricRegistrationToken.class);
        // 算法白名单：当前跨端协议 v1 只接受 EC_P256_SHA256。
        if (registration == null || !user.getUid().equals(registration.getUid())
            || algorithm != SignAlgorithmEnum.EC_P256_SHA256) {
            throw PaySecurityException.of(PaySecurityErrorCode.BIOMETRIC_REGISTRATION_INVALID);
        }
        UserSecuritySettings security = paySecurityRuntime.getSettings(user.getUid());
        if (security == null || security.getPasswordStatus() != PayPasswordStatusEnum.ENABLED
            || !security.getSecurityVersion().equals(registration.getSecurityVersion())
            || !security.getPasswordVersion().equals(registration.getPasswordVersion())) {
            throw PaySecurityException.of(PaySecurityErrorCode.PAY_SECURITY_STATE_CHANGED);
        }
        PublicKey parsedKey = paySecurityRuntime.parseP256PublicKey(publicKey);
        // 注册签名只证明客户端持有上传公钥对应的私钥，硬件证明状态按当前残余风险记录为未验证。
        paySecurityRuntime.verifySignature(parsedKey, registration.getNonce(), signature);
        // 占用失败说明并发注册正在进行：优先幂等返回已落库凭证，仍未落库则按会话已使用拒绝。
        String registrationUsedKey = paySecurityRuntime.getRedisKeys().registrationUsed(registrationId);
        if (!paySecurityRuntime.getPaySecurityKeyValueStore().setIfAbsent(registrationUsedKey, "1",
            paySecurityRuntime.authorizationTtlSeconds())) {
            existing = paySecurityRuntime.getBiometricCredentialRepository().findByUidAndRegistrationId(user.getUid(), registrationId);
            if (existing != null) {
                return paySecurityRuntime.credentialView(existing);
            }
            throw PaySecurityException.of(PaySecurityErrorCode.BIOMETRIC_REGISTRATION_USED);
        }
        BiometricCredential credential = new BiometricCredential();
        credential.setCredentialId(UUID.randomUUID().toString());
        credential.setRegistrationId(registrationId);
        credential.setUid(user.getUid());
        credential.setPlatform(registration.getPlatform());
        credential.setPasswordVersion(registration.getPasswordVersion());
        credential.setSecurityVersion(registration.getSecurityVersion());
        credential.setPublicKey(publicKey);
        credential.setAlgorithm(algorithm);
        credential.setKeyAttestationStatus(AttestationStatusEnum.UNVERIFIED);
        credential.setAppIntegrityStatus(AttestationStatusEnum.UNVERIFIED);
        credential.setStatus(CredentialStatusEnum.ENABLED);
        credential.setRegisteredAt(new Date());
        paySecurityRuntime.getBiometricCredentialRepository().insert(credential);
        paySecurityRuntime.getPaySecurityKeyValueStore().delete(registrationKey);
        paySecurityRuntime.audit(AuditEventEnum.BIOMETRIC_CREDENTIAL_REGISTER, user.getUid(),
            registration.getOrderNo(), credential.getCredentialId(), AuditResultEnum.SUCCESS, null, null,
            null, clientInfo);
        return paySecurityRuntime.credentialView(credential);
    }

    /**
     * 订单绑定模式的签名挑战，等价于金额参数为空的
     * {@link #createChallenge(UserIdentity, String, String, String, BigDecimal, String)}。
     */
    public BiometricChallenge createChallenge(UserIdentity user, String credentialId, String orderNo,
        String orderType) {
        return createChallenge(user, credentialId, orderNo, orderType, null, null);
    }

    /**
     * 创建与用户、Credential和支付意图绑定的一次性签名挑战。
     *
     * <p>传订单号时挑战金额取服务端实时订单金额（客户端不可篡改）；订单号为空时按声明
     * 金额+币种生成"金额授权"挑战（订单号在支付请求内才生成的流程使用），
     * 消费侧仍会用实际订单金额与票据金额强校验。</p>
     *
     * <p>流程节点：用户激活校验 → 锁定检查 → 凭证可用性（状态+密码版本+安全版本三元组）
     * → 解析支付意图 → 生成载荷完整绑定的挑战（TTL 120 秒）。</p>
     *
     * @param user 当前登录用户
     * @param credentialId 生物支付凭证ID
     * @param orderNo 待支付订单号，可为空（金额授权模式）
     * @param orderType 订单类型
     * @param amount 订单号为空时必填的授权金额
     * @param currency 订单号为空时必填的币种
     * @return 一次性签名挑战
     */
    public BiometricChallenge createChallenge(UserIdentity user, String credentialId, String orderNo,
        String orderType, BigDecimal amount, String currency) {
        paySecurityRuntime.ensureActiveUser(user);
        paySecurityRuntime.getLockoutManager().ensureNotLocked(LockoutScopeEnum.BALANCE_PAY, user.getUid());
        BiometricCredential credential = paySecurityRuntime.getEnabledCredential(user.getUid(), credentialId);
        OrderPaymentInfo order = paySecurityRuntime.resolveIntent(user.getUid(), orderNo, orderType, amount, currency);
        PayBiometricChallengeToken challenge = new PayBiometricChallengeToken();
        // 签名载荷同时绑定用户、Credential、订单、金额、币种和有效期。
        challenge.setUid(user.getUid());
        challenge.setCredentialId(credential.getCredentialId());
        challenge.setOrderNo(order.getOrderNo());
        challenge.setOrderType(order.getOrderType());
        challenge.setAmount(order.getAmount());
        challenge.setCurrency(order.getCurrency());
        challenge.setNonce(paySecurityRuntime.randomToken());
        long challengeTtlSeconds = paySecurityRuntime.challengeTtlSeconds();
        challenge.setExpiresAt(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(challengeTtlSeconds));
        String challengeId = UUID.randomUUID().toString();
        String challengeKey = paySecurityRuntime.getRedisKeys().challenge(challengeId);
        String challengeJson = paySecurityRuntime.getJsonHelper().toJson(challenge);
        if (!paySecurityRuntime.getPaySecurityKeyValueStore().set(challengeKey, challengeJson, challengeTtlSeconds)) {
            throw PaySecurityException.of(PaySecurityErrorCode.PAY_SECURITY_UNAVAILABLE);
        }
        BiometricChallenge result = new BiometricChallenge();
        result.setChallengeId(challengeId);
        result.setSignPayload(PaySecurityChecks.challengePayload(challengeId, challenge));
        result.setExpiresIn(challengeTtlSeconds);
        return result;
    }

    /**
     * 验证挑战签名并签发订单绑定的支付授权票据。
     *
     * <p>流程节点：锁定检查 → 挑战归属与有效期核对 → 凭证可用性复核 → 存储公钥验签
     * → 原子占用挑战（一次验签一次消费，防签名重放）→ 更新凭证最近使用时间 → 审计并签发支付授权票据；
     * 占用后任一步骤失败即释放占用，客户端未拿到票据，同一挑战在有效期内可重试。</p>
     *
     * @param user 当前登录用户
     * @param challengeId 一次性生物签名挑战ID，验签成功后立即消费
     * @param credentialId 生物支付凭证ID
     * @param signature 客户端私钥对signPayload生成的Base64 DER ECDSA签名
     * @param clientInfo 客户端环境快照
     * @return 支付授权结果
     */
    public AuthorizationResult authorize(UserIdentity user, String challengeId, String credentialId,
        String signature, ClientInfo clientInfo) {
        paySecurityRuntime.ensureActiveUser(user);
        paySecurityRuntime.getLockoutManager().ensureNotLocked(LockoutScopeEnum.BALANCE_PAY, user.getUid());
        String challengeKey = paySecurityRuntime.getRedisKeys().challenge(challengeId);
        PayBiometricChallengeToken challenge = paySecurityRuntime.getJsonHelper()
            .fromJson(paySecurityRuntime.getPaySecurityKeyValueStore().get(challengeKey), PayBiometricChallengeToken.class);
        // 挑战必须归属当前用户与凭证且未过期；过期挑战即使签名正确也不能换取授权。
        if (challenge == null || !user.getUid().equals(challenge.getUid())
            || !credentialId.equals(challenge.getCredentialId())
            || challenge.getExpiresAt() < System.currentTimeMillis()) {
            throw PaySecurityException.of(PaySecurityErrorCode.BIOMETRIC_CHALLENGE_INVALID);
        }
        BiometricCredential credential = paySecurityRuntime.getEnabledCredential(user.getUid(), credentialId);
        paySecurityRuntime.verifySignature(paySecurityRuntime.parseP256PublicKey(credential.getPublicKey()),
            PaySecurityChecks.challengePayload(challengeId, challenge), signature);
        // 验签通过后原子占用挑战：一次验签一次消费，历史签名不能重放换取新授权。
        String challengeUsedKey = paySecurityRuntime.getRedisKeys().challengeUsed(challengeId);
        if (!paySecurityRuntime.getPaySecurityKeyValueStore().setIfAbsent(challengeUsedKey, "1",
            paySecurityRuntime.challengeTtlSeconds())) {
            throw PaySecurityException.of(PaySecurityErrorCode.BIOMETRIC_CHALLENGE_USED);
        }
        try {
            // 只写 last_used_at 单字段：全量回写会把并发撤销/停用后的状态覆盖回 ENABLED。
            paySecurityRuntime.getBiometricCredentialRepository().updateLastUsedAt(user.getUid(), credential.getCredentialId(), new Date());
            UserSecuritySettings security = paySecurityRuntime.getSettings(user.getUid());
            paySecurityRuntime.audit(AuditEventEnum.BIOMETRIC_AUTHORIZE, user.getUid(), challenge.getOrderNo(),
                credential.getCredentialId(), AuditResultEnum.SUCCESS, null, challenge.getAmount(),
                challenge.getCurrency(), clientInfo);
            return paySecurityRuntime.issueAuthorization(user, security, challenge.getOrderNo(), challenge.getOrderType(),
                challenge.getAmount(), challenge.getCurrency(), AuthorizeMethodEnum.BIOMETRIC,
                credential.getCredentialId(), clientInfo);
        } catch (RuntimeException e) {
            // 占用后未能把票据交到客户端手上：释放占用允许同一挑战重试；签名已验过，无票据在手不构成重放获利。
            paySecurityRuntime.getPaySecurityKeyValueStore().delete(challengeUsedKey);
            throw e;
        }
    }

    /**
     * 查询当前用户的全部生物支付凭证。
     *
     * @param user 当前登录用户
     * @return 生物支付凭证列表
     */
    public List<BiometricCredentialInfo> listCredentials(UserIdentity user) {
        List<BiometricCredentialInfo> views = new ArrayList<>();
        for (BiometricCredential credential : paySecurityRuntime.getBiometricCredentialRepository().findByUid(user.getUid())) {
            views.add(paySecurityRuntime.credentialView(credential));
        }
        return views;
    }

    /**
     * 撤销当前用户拥有的指定生物支付凭证（已撤销时幂等返回）。
     *
     * @param user 当前登录用户
     * @param credentialId 生物支付凭证ID
     * @param clientInfo 客户端环境快照
     */
    public void revokeCredential(UserIdentity user, String credentialId, ClientInfo clientInfo) {
        paySecurityRuntime.ensureActiveUser(user);
        BiometricCredential credential = paySecurityRuntime.getOwnedCredential(user.getUid(), credentialId);
        if (credential.getStatus() == CredentialStatusEnum.REVOKED) {
            return;
        }
        // 条件部分更新：并发重复撤销只有一个生效，且不会用旧快照覆盖已撤销记录的原因与时间。
        if (paySecurityRuntime.getBiometricCredentialRepository().revokeByUidAndCredentialId(user.getUid(), credentialId,
            CredentialStatusEnum.REVOKED, CredentialDisabledReasonEnum.USER_UNBOUND, new Date()) == 0) {
            return;
        }
        paySecurityRuntime.audit(AuditEventEnum.BIOMETRIC_CREDENTIAL_REVOKE, user.getUid(), null, credentialId,
            AuditResultEnum.SUCCESS, CredentialDisabledReasonEnum.USER_UNBOUND, null, null, clientInfo);
    }

    /**
     * 查询指定凭证的可用状态。
     *
     * @param user 当前登录用户
     * @param credentialId 生物支付凭证ID
     * @return 生物支付凭证状态
     */
    public BiometricCredentialInfo getCredentialStatus(UserIdentity user, String credentialId) {
        BiometricCredential credential = paySecurityRuntime.getOwnedCredential(user.getUid(), credentialId);
        return paySecurityRuntime.credentialView(credential);
    }

    /**
     * 撤销当前用户全部生物凭证并递增支付安全版本，使全部历史票据立即失效。
     *
     * <p>宿主门面应在事务内调用本方法。</p>
     *
     * @param user 当前登录用户
     * @param clientInfo 客户端环境快照
     */
    public void revokeAll(UserIdentity user, ClientInfo clientInfo) {
        paySecurityRuntime.ensureActiveUser(user);
        paySecurityRuntime.disableAllCredentials(user.getUid(), CredentialDisabledReasonEnum.USER_REVOKE_ALL);
        UserSecuritySettings security = paySecurityRuntime.getSettings(user.getUid());
        if (security != null) {
            // 全局安全版本递增：全部历史支付票据与身份票据随之立即失效。
            // 版本 CAS：与并发的改密/另一次撤销互斥，防止旧快照回写吞掉对方的版本递增。
            int expectedSecurityVersion = security.getSecurityVersion();
            security.setSecurityVersion(expectedSecurityVersion + 1);
            if (!paySecurityRuntime.getUserSecuritySettingsRepository().updateWithVersion(security, expectedSecurityVersion)) {
                throw PaySecurityException.of(PaySecurityErrorCode.PAY_SECURITY_STATE_CHANGED);
            }
        }
        paySecurityRuntime.audit(AuditEventEnum.BIOMETRIC_CREDENTIAL_REVOKE_ALL, user.getUid(), null, null,
            AuditResultEnum.SUCCESS, null, null, null, clientInfo);
    }

}
