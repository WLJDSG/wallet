package com.wallet.security.service;

import com.wallet.security.config.PaySecurityProperties;
import lombok.AccessLevel;
import lombok.Getter;
import com.wallet.security.core.Hashes;
import com.wallet.security.core.LockoutManager;
import com.wallet.security.core.PayBiometricCrypto;
import com.wallet.security.core.PaySecurityConstants;
import com.wallet.security.enums.AuditEventEnum;
import com.wallet.security.enums.AuditResultEnum;
import com.wallet.security.enums.AuthorizeMethodEnum;
import com.wallet.security.enums.BiometricPlatformEnum;
import com.wallet.security.enums.CredentialDisabledReasonEnum;
import com.wallet.security.enums.CredentialStatusEnum;
import com.wallet.security.enums.IdentityPurposeEnum;
import com.wallet.security.enums.PasswordAuditEvent;
import com.wallet.security.enums.PayPasswordStatusEnum;
import com.wallet.security.enums.ProtocolValue;
import com.wallet.security.core.RedisKeys;
import com.wallet.security.core.SmsRateLimiter;
import com.wallet.security.core.Texts;
import com.wallet.security.core.JsonHelper;
import com.wallet.security.error.PaySecurityErrorCode;
import com.wallet.security.error.PaySecurityException;
import com.wallet.security.model.AuthorizationResult;
import com.wallet.security.model.BiometricCredentialInfo;
import com.wallet.security.model.ClientInfo;
import com.wallet.security.model.OrderPaymentInfo;
import com.wallet.security.model.UserIdentity;
import com.wallet.security.spi.AuditRecorder;
import com.wallet.security.spi.BiometricCredentialRepository;
import com.wallet.security.spi.OrderPaymentInfoResolver;
import com.wallet.security.spi.PaySecurityKeyValueStore;
import com.wallet.security.spi.UserSecuritySettingsRepository;
import com.wallet.security.spi.SmsSender;
import com.wallet.security.spi.model.BiometricCredential;
import com.wallet.security.spi.model.PaySecurityAuditEvent;
import com.wallet.security.spi.model.UserSecuritySettings;
import com.wallet.security.token.PayAuthorizationToken;
import com.wallet.security.token.PayBiometricRegistrationToken;
import com.wallet.security.token.PayIdentityToken;
import com.wallet.security.core.BcryptHasher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.time.ZoneId;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * 域服务共享运行时（支付安全内核 内部类，请勿在宿主代码中直接使用）。
 *
 * <p>集中承载票据签发、审计与凭证归属校验等跨域共享逻辑；
 * 四个域服务通过它协作，宿主只应使用 {@code PaySecurityEngine} 暴露的服务。</p>
 */
@Getter(AccessLevel.PACKAGE)
public final class PaySecurityRuntime {

    private static final Logger log = LoggerFactory.getLogger(PaySecurityRuntime.class);

    private final PaySecurityProperties paySecurityProperties;
    private final ZoneId businessZone;
    private final PaySecurityKeyValueStore paySecurityKeyValueStore;
    private final JsonHelper jsonHelper;
    private final RedisKeys redisKeys;
    private final UserSecuritySettingsRepository userSecuritySettingsRepository;
    private final BiometricCredentialRepository biometricCredentialRepository;
    private final AuditRecorder auditRecorder;
    private final SmsSender smsSender;
    private final OrderPaymentInfoResolver orderPaymentInfoResolver;
    private final LockoutManager lockoutManager;
    private final SmsRateLimiter smsRateLimiter;
    private final BcryptHasher bcryptHasher;
    private final SecureRandom secureRandom = new SecureRandom();

    public PaySecurityRuntime(PaySecurityProperties paySecurityProperties, PaySecurityKeyValueStore paySecurityKeyValueStore,
        UserSecuritySettingsRepository userSecuritySettingsRepository, BiometricCredentialRepository biometricCredentialRepository,
        AuditRecorder auditRecorder, SmsSender smsSender, OrderPaymentInfoResolver orderPaymentInfoResolver) {
        this.paySecurityProperties = paySecurityProperties;
        this.businessZone = ZoneId.of(paySecurityProperties.getBusinessZoneId());
        this.paySecurityKeyValueStore = paySecurityKeyValueStore;
        this.jsonHelper = new JsonHelper();
        this.redisKeys = new RedisKeys(paySecurityProperties.getRedisKeyPrefix(), businessZone);
        this.userSecuritySettingsRepository = userSecuritySettingsRepository;
        this.biometricCredentialRepository = biometricCredentialRepository;
        this.auditRecorder = auditRecorder;
        this.smsSender = smsSender;
        this.orderPaymentInfoResolver = orderPaymentInfoResolver;
        this.lockoutManager = new LockoutManager(paySecurityKeyValueStore, redisKeys, paySecurityProperties, businessZone);
        this.smsRateLimiter = new SmsRateLimiter(paySecurityKeyValueStore, redisKeys, paySecurityProperties, businessZone);
        this.bcryptHasher = new BcryptHasher(paySecurityProperties.getBcryptStrength());
    }

    long authorizationTtlSeconds() {
        return paySecurityProperties.getAuthorizationTtlSeconds();
    }

    long challengeTtlSeconds() {
        return paySecurityProperties.getChallengeTtlSeconds();
    }

    String randomToken() {
        return Hashes.randomToken(secureRandom);
    }

    /** 生成 6 位（无前导零）安全随机短信验证码。 */
    String generateSmsCode() {
        return String.valueOf(100000 + secureRandom.nextInt(900000));
    }

    /** 手机号频控键的 HMAC 摘要：带宿主配置的密钥，防止无盐哈希被彩虹表/枚举还原手机号。 */
    String phoneHash(String phone) {
        return Hashes.hmacSha256(paySecurityProperties.getPhoneHashPepper(), phone);
    }

    /** 签发票据与变更安全状态的入口统一校验：用户必须存在且处于激活状态。 */
    void ensureActiveUser(UserIdentity user) {
        if (user == null || !user.isActive()) {
            throw PaySecurityException.of(PaySecurityErrorCode.PAY_SECURITY_USER_INVALID);
        }
    }

    UserSecuritySettings getSettings(Long uid) {
        return userSecuritySettingsRepository.findByUid(uid);
    }

    /**
     * 解析订单事实并统一空值语义：不存在或不归属该用户一律抛 ORDER_DOES_NOT_EXIST。
     * 返回的快照强制绑定入参订单号与类型，防止实现回填不一致。
     */
    OrderPaymentInfo resolveOrder(Long uid, String orderNo, String orderType) {
        OrderPaymentInfo facts = orderPaymentInfoResolver.resolve(uid, orderNo, orderType);
        if (facts == null) {
            throw PaySecurityException.of(PaySecurityErrorCode.ORDER_DOES_NOT_EXIST);
        }
        return new OrderPaymentInfo(orderNo, orderType, facts.getAmount(), facts.getCurrency(), facts.getPaid());
    }

    /**
     * 解析支付意图：传订单号即实时复核订单（未支付），否则按客户端声明金额构造无订单快照。
     * 金额授权模式要求金额为正、币种非空，快照 orderNo/orderType 为 null 表示不做订单号绑定。
     */
    OrderPaymentInfo resolveIntent(Long uid, String orderNo, String orderType, BigDecimal amount, String currency) {
        if (Texts.isNotBlank(orderNo)) {
            OrderPaymentInfo order = resolveOrder(uid, orderNo, orderType);
            if (Boolean.TRUE.equals(order.getPaid())) {
                throw PaySecurityException.of(PaySecurityErrorCode.ORDER_PAID);
            }
            return order;
        }
        if (amount == null || amount.signum() <= 0 || Texts.isBlank(currency)) {
            throw PaySecurityException.of(PaySecurityErrorCode.PAY_SECURITY_AMOUNT_INVALID);
        }
        return new OrderPaymentInfo(null, null, amount, currency, false);
    }

    /**
     * 复核支付意图后签发一次性支付授权票据。
     *
     * <p>两种绑定模式：传订单号时实时复核订单（必须存在、归属当前用户、未支付），
     * 票据与订单强绑定；订单号为空时按客户端声明的金额+币种签发"金额授权"票据
     * （用于订单号在支付请求内才生成的流程），消费侧仍会用实际订单金额与票据金额强校验，
     * 谎报金额只会导致消费失败。票据原文只回传客户端，存储只落 sha256；
     * 签发成功审计 PAY_AUTH_ISSUE。</p>
     */
    AuthorizationResult issueAuthorization(UserIdentity user, UserSecuritySettings security, String orderNo,
        String orderType, BigDecimal amount, String currency, AuthorizeMethodEnum method, String credentialId,
        ClientInfo clientInfo) {
        OrderPaymentInfo order = resolveIntent(user.getUid(), orderNo, orderType, amount, currency);
        PayAuthorizationToken authorization = new PayAuthorizationToken();
        authorization.setUid(user.getUid());
        authorization.setOrderNo(order.getOrderNo());
        authorization.setOrderType(order.getOrderType());
        authorization.setAmount(order.getAmount());
        authorization.setCurrency(order.getCurrency());
        authorization.setAuthorizationMethod(method);
        authorization.setCredentialId(credentialId);
        authorization.setPasswordVersion(security == null ? null : security.getPasswordVersion());
        authorization.setSecurityVersion(security == null ? 1 : security.getSecurityVersion());
        authorization.setPolicyVersion(PaySecurityConstants.POLICY_VERSION);
        authorization.setIssuedAt(System.currentTimeMillis());
        authorization.setExpiresAt(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(authorizationTtlSeconds()));
        String token = randomToken();
        if (!paySecurityKeyValueStore.set(redisKeys.authorization(Hashes.sha256(token)), jsonHelper.toJson(authorization),
            authorizationTtlSeconds())) {
            throw PaySecurityException.of(PaySecurityErrorCode.PAY_SECURITY_UNAVAILABLE);
        }
        AuthorizationResult result = new AuthorizationResult();
        result.setPayAuthorizationToken(token);
        result.setExpiresIn(authorizationTtlSeconds());
        audit(AuditEventEnum.PAY_AUTH_ISSUE, user.getUid(), orderNo, credentialId,
            AuditResultEnum.SUCCESS, method, order.getAmount(), order.getCurrency(), clientInfo);
        return result;
    }

    /**
     * 签发一次性身份票据；用途为 BIOMETRIC_ENROLLMENT 时改为签发生物注册票据
     * （额外绑定原生平台、要求已设密码、来源订单需真实存在，且与普通身份票据分键存储）。
     */
    String issueIdentityToken(Long uid, IdentityPurposeEnum purpose, UserSecuritySettings security, String orderNo,
        String orderType, ClientInfo clientInfo) {
        PayIdentityToken token = new PayIdentityToken();
        token.setUid(uid);
        token.setPurpose(purpose);
        token.setPasswordVersion(security == null ? null : security.getPasswordVersion());
        token.setSecurityVersion(security == null ? 1 : security.getSecurityVersion());
        token.setOrderNo(orderNo);
        token.setOrderType(orderType);
        String raw = randomToken();
        if (purpose == IdentityPurposeEnum.BIOMETRIC_ENROLLMENT) {
            if (security == null) {
                throw PaySecurityException.of(PaySecurityErrorCode.PAY_IDENTITY_TOKEN_INVALID);
            }
            if (Texts.isNotBlank(orderNo)) {
                resolveOrder(uid, orderNo, orderType);
            }
            PayBiometricRegistrationToken enrollment = new PayBiometricRegistrationToken();
            enrollment.setUid(uid);
            enrollment.setOrderNo(orderNo);
            enrollment.setOrderType(orderType);
            enrollment.setPlatform(normalizePlatform(clientInfo == null ? null : clientInfo.getSystem()));
            enrollment.setPasswordVersion(security.getPasswordVersion());
            enrollment.setSecurityVersion(security.getSecurityVersion());
            if (!paySecurityKeyValueStore.set(redisKeys.enrollment(Hashes.sha256(raw)), jsonHelper.toJson(enrollment),
                authorizationTtlSeconds())) {
                throw PaySecurityException.of(PaySecurityErrorCode.PAY_SECURITY_UNAVAILABLE);
            }
        } else if (!paySecurityKeyValueStore.set(redisKeys.identity(Hashes.sha256(raw)), jsonHelper.toJson(token), authorizationTtlSeconds())) {
            throw PaySecurityException.of(PaySecurityErrorCode.PAY_SECURITY_UNAVAILABLE);
        }
        return raw;
    }

    /** 支付成功场景顺带签发的生物注册票据（绑定来源订单，开通会话建立前会复核该订单已支付）。 */
    String issueEnrollmentToken(UserIdentity user, UserSecuritySettings security, String orderNo, String orderType,
        ClientInfo clientInfo) {
        PayBiometricRegistrationToken enrollment = new PayBiometricRegistrationToken();
        enrollment.setUid(user.getUid());
        enrollment.setOrderNo(orderNo);
        enrollment.setOrderType(orderType);
        enrollment.setPlatform(normalizePlatform(clientInfo == null ? null : clientInfo.getSystem()));
        enrollment.setPasswordVersion(security.getPasswordVersion());
        enrollment.setSecurityVersion(security.getSecurityVersion());
        String token = randomToken();
        if (!paySecurityKeyValueStore.set(redisKeys.enrollment(Hashes.sha256(token)), jsonHelper.toJson(enrollment),
            authorizationTtlSeconds())) {
            throw PaySecurityException.of(PaySecurityErrorCode.PAY_SECURITY_UNAVAILABLE);
        }
        return token;
    }

    void audit(ProtocolValue eventType, Long uid, String orderNo, String credentialId, AuditResultEnum result,
        Object reasonCode, BigDecimal amount, String currency, ClientInfo clientInfo) {
        try {
            PaySecurityAuditEvent event = new PaySecurityAuditEvent();
            event.setEventType(eventType);
            event.setUid(uid);
            event.setOrderNo(orderNo);
            event.setCredentialId(credentialId);
            event.setClientType(clientInfo == null ? null : clientInfo.getPlatform());
            event.setAppVersion(clientInfo == null ? null : clientInfo.getAppVersion());
            event.setResult(result);
            event.setReasonCode(reasonCode == null ? null : reasonCode.toString());
            event.setAmount(amount);
            event.setCurrency(currency);
            if (clientInfo != null) {
                event.setIp(clientInfo.getIp());
                String userAgent = clientInfo.getUserAgent();
                event.setUserAgentDigest(Texts.isBlank(userAgent) ? null : Hashes.sha256(userAgent));
            }
            event.setOccurredAt(new Date());
            auditRecorder.record(event);
        } catch (Exception e) {
            // 审计写入失败不能绕过已经完成的授权校验，但必须留痕供监控告警。
            log.error("支付安全审计写入失败，eventType={}, uid={}, orderNo={}", eventType, uid, orderNo, e);
        }
    }

    boolean isCredentialAvailable(Long uid, String credentialId, UserSecuritySettings security) {
        BiometricCredential credential = biometricCredentialRepository.findByUidAndCredentialId(uid, credentialId);
        return credential != null && security != null
            && credential.getStatus() == CredentialStatusEnum.ENABLED
            && security.getPasswordVersion().equals(credential.getPasswordVersion())
            && security.getSecurityVersion().equals(credential.getSecurityVersion());
    }

    /** 凭证可用性三元组校验：status=ENABLED 且密码版本、安全版本均与当前档案一致，缺一不可。 */
    BiometricCredential getEnabledCredential(Long uid, String credentialId) {
        BiometricCredential credential = getOwnedCredential(uid, credentialId);
        UserSecuritySettings security = getSettings(uid);
        if (credential.getStatus() != CredentialStatusEnum.ENABLED || security == null
            || !security.getPasswordVersion().equals(credential.getPasswordVersion())
            || !security.getSecurityVersion().equals(credential.getSecurityVersion())) {
            throw PaySecurityException.of(PaySecurityErrorCode.BIOMETRIC_CREDENTIAL_INVALID);
        }
        return credential;
    }

    BiometricCredential getOwnedCredential(Long uid, String credentialId) {
        BiometricCredential credential = biometricCredentialRepository.findByUidAndCredentialId(uid, credentialId);
        if (credential == null) {
            throw PaySecurityException.of(PaySecurityErrorCode.BIOMETRIC_CREDENTIAL_INVALID);
        }
        return credential;
    }

    BiometricCredentialInfo credentialView(BiometricCredential credential) {
        BiometricCredentialInfo view = new BiometricCredentialInfo();
        view.setCredentialId(credential.getCredentialId());
        view.setPlatform(credential.getPlatform());
        view.setStatus(credential.getStatus());
        UserSecuritySettings security = getSettings(credential.getUid());
        view.setAvailable(credential.getStatus() == CredentialStatusEnum.ENABLED && security != null
            && security.getPasswordVersion().equals(credential.getPasswordVersion())
            && security.getSecurityVersion().equals(credential.getSecurityVersion()));
        view.setLastUsedAt(credential.getLastUsedAt());
        return view;
    }

    void disableAllCredentials(Long uid, CredentialDisabledReasonEnum reason) {
        biometricCredentialRepository.disableAllEnabledByUid(uid, CredentialStatusEnum.ENABLED, CredentialStatusEnum.DISABLED,
            reason, new Date());
    }

    void validateSmsPurpose(Long uid, IdentityPurposeEnum purpose) {
        UserSecuritySettings security = getSettings(uid);
        boolean passwordSet = security != null && security.getPasswordStatus() == PayPasswordStatusEnum.ENABLED;
        if ((purpose != IdentityPurposeEnum.PASSWORD_SET && purpose != IdentityPurposeEnum.PASSWORD_RESET)
            || (purpose == IdentityPurposeEnum.PASSWORD_SET && passwordSet)
            || (purpose == IdentityPurposeEnum.PASSWORD_RESET && !passwordSet)) {
            throw PaySecurityException.of(PaySecurityErrorCode.PAY_IDENTITY_PURPOSE_INVALID);
        }
    }

    BiometricPlatformEnum normalizePlatform(String platform) {
        if ("android".equalsIgnoreCase(platform)) {
            return BiometricPlatformEnum.ANDROID;
        }
        if ("ios".equalsIgnoreCase(platform)) {
            return BiometricPlatformEnum.IOS;
        }
        throw PaySecurityException.of(PaySecurityErrorCode.PAY_SECURITY_CLIENT_INVALID);
    }

    PublicKey parseP256PublicKey(String encodedPublicKey) {
        try {
            return PayBiometricCrypto.parseP256PublicKey(encodedPublicKey);
        } catch (IllegalArgumentException e) {
            throw PaySecurityException.of(PaySecurityErrorCode.BIOMETRIC_CREDENTIAL_INVALID);
        }
    }

    void verifySignature(PublicKey publicKey, String payload, String encodedSignature) {
        if (!PayBiometricCrypto.verifyDerSignature(publicKey, payload, encodedSignature)) {
            throw PaySecurityException.of(PaySecurityErrorCode.BIOMETRIC_SIGNATURE_INVALID);
        }
    }
}
