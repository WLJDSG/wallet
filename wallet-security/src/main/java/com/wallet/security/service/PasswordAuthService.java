package com.wallet.security.service;

import com.wallet.security.enums.AuditEventEnum;
import com.wallet.security.enums.AuthorizeMethodEnum;
import com.wallet.security.enums.AuditResultEnum;
import com.wallet.security.enums.LockoutScopeEnum;
import com.wallet.security.enums.PayPasswordStatusEnum;
import com.wallet.security.error.PaySecurityErrorCode;
import com.wallet.security.error.PaySecurityException;
import com.wallet.security.model.AuthorizationResult;
import com.wallet.security.model.ClientInfo;
import com.wallet.security.model.SecurityStatus;
import com.wallet.security.model.UserIdentity;
import com.wallet.security.spi.model.UserSecuritySettings;
import com.wallet.security.core.PaySecurityConstants;
import com.wallet.security.core.Texts;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 支付安全状态查询与密码/二次确认授权。
 *
 * <p>密码或二次确认校验通过后只签发订单绑定的一次性支付授权票据，
 * 最终余额扣减必须由 {@link AuthorizationService#consume} 复核并消费票据。</p>
 */
public final class PasswordAuthService {

    private final PaySecurityRuntime paySecurityRuntime;

    /** 内部构造器，宿主请通过 {@code PaySecurityEngine} 装配。 */
    public PasswordAuthService(PaySecurityRuntime paySecurityRuntime) {
        this.paySecurityRuntime = paySecurityRuntime;
    }

    /**
     * 查询当前用户和客户端指定凭证对应的支付安全状态。
     *
     * <p>流程节点：读取密码档案与锁定状态 → 指定凭证可用性判定
     * → 推荐授权方式决策（生物 &gt; 密码 &gt; 二次确认兜底）。</p>
     *
     * @param user 当前登录用户
     * @param credentialId 客户端本地保存的生物支付凭证ID，可为空
     * @return 服务端计算的支付安全状态，只供前端选择交互流程
     */
    public SecurityStatus getStatus(UserIdentity user, String credentialId) {
        UserSecuritySettings security = paySecurityRuntime.getSettings(user.getUid());
        Date lockedUntil = paySecurityRuntime.getLockoutManager().getLockedUntil(LockoutScopeEnum.BALANCE_PAY, user.getUid());
        // 客户端必须用本地私钥对应的 credentialId 查询，服务端只判断该公钥凭证及安全版本是否有效。
        boolean biometricAvailable = Texts.isNotBlank(credentialId)
            && paySecurityRuntime.isCredentialAvailable(user.getUid(), credentialId, security);
        SecurityStatus status = new SecurityStatus();
        status.setPasswordSet(security != null && security.getPasswordStatus() == PayPasswordStatusEnum.ENABLED);
        status.setBalancePayLocked(lockedUntil != null);
        status.setLockedUntil(lockedUntil);
        // 锁定期间不推荐生物支付，统一引导用户走锁定提示。
        status.setBiometricAvailable(biometricAvailable && lockedUntil == null);
        status.setPreferredMethod(Boolean.TRUE.equals(status.getBiometricAvailable())
            ? AuthorizeMethodEnum.BIOMETRIC
            : Boolean.TRUE.equals(status.getPasswordSet()) ? AuthorizeMethodEnum.PASSWORD
                : AuthorizeMethodEnum.LEGACY_CONFIRM);
        status.setSecurityPolicyVersion(PaySecurityConstants.POLICY_VERSION);
        return status;
    }

    /**
     * 订单绑定模式的支付密码授权，等价于金额参数为空的
     * {@link #authorizePassword(UserIdentity, String, String, BigDecimal, String, String, ClientInfo)}。
     */
    public AuthorizationResult authorizePassword(UserIdentity user, String orderNo, String orderType,
        String password, ClientInfo clientInfo) {
        return authorizePassword(user, orderNo, orderType, null, null, password, clientInfo);
    }

    /**
     * 校验支付密码并签发支付授权票据。
     *
     * <p>传订单号时票据与订单快照强绑定；订单号为空时按声明金额+币种签发"金额授权"票据
     * （订单号在支付请求内才生成的流程使用），消费侧仍会用实际订单金额与票据金额强校验。</p>
     *
     * <p>流程节点：用户激活校验 → 密码已设校验 → 锁定检查 → BCrypt 校验 → 失败计数与锁定升级 /
     * 成功清连续计数 → 复核支付意图并签发一次性票据 → 审计 → 原生 App 渠道顺带签发生物注册票据。</p>
     *
     * @param user 当前登录用户
     * @param orderNo 待支付订单号，可为空（金额授权模式）
     * @param orderType 订单类型
     * @param amount 订单号为空时必填的授权金额
     * @param currency 订单号为空时必填的币种
     * @param password 6位支付密码明文
     * @param clientInfo 客户端环境快照
     * @return 一次性支付授权结果
     */
    public AuthorizationResult authorizePassword(UserIdentity user, String orderNo, String orderType,
        BigDecimal amount, String currency, String password, ClientInfo clientInfo) {
        paySecurityRuntime.ensureActiveUser(user);
        UserSecuritySettings security = paySecurityRuntime.getSettings(user.getUid());
        if (security == null || security.getPasswordStatus() != PayPasswordStatusEnum.ENABLED) {
            throw PaySecurityException.of(PaySecurityErrorCode.PAY_PASSWORD_NOT_SET);
        }
        // 锁定检查先于密码校验：锁定期间直接拒绝，不给继续试探密码的机会。
        paySecurityRuntime.getLockoutManager().ensureNotLocked(LockoutScopeEnum.BALANCE_PAY, user.getUid());
        if (!paySecurityRuntime.getBcryptHasher().matches(password, security.getPasswordHash())) {
            // 连续失败与当日累计失败分别计数，任一达到阈值都立即锁定余额支付。
            int remainingAttempts = paySecurityRuntime.getLockoutManager()
                .recordPasswordFailure(LockoutScopeEnum.BALANCE_PAY, user.getUid());
            paySecurityRuntime.audit(AuditEventEnum.PASSWORD_AUTHORIZE, user.getUid(), orderNo, null,
                AuditResultEnum.FAIL, PaySecurityErrorCode.PAY_PASSWORD_ERROR.getCode(), null, null, clientInfo);
            // 本次失败恰好触发锁定时，直接按锁定报错而非剩余次数。
            paySecurityRuntime.getLockoutManager().ensureNotLocked(LockoutScopeEnum.BALANCE_PAY, user.getUid());
            throw PaySecurityException.of(PaySecurityErrorCode.PAY_PASSWORD_ERROR, remainingAttempts);
        }
        // 密码正确只清连续失败计数；当日累计计数保留，跨会话反复试探仍会触发当日上限。
        paySecurityRuntime.getLockoutManager().clearContinuousFailures(LockoutScopeEnum.BALANCE_PAY, user.getUid());
        AuthorizationResult result = paySecurityRuntime.issueAuthorization(user, security, orderNo, orderType, amount, currency,
            AuthorizeMethodEnum.PASSWORD, null, clientInfo);
        paySecurityRuntime.audit(AuditEventEnum.PASSWORD_AUTHORIZE, user.getUid(), orderNo, null,
            AuditResultEnum.SUCCESS, null, null, null, clientInfo);
        // 原生 App 复用本次密码校验顺带签发生物注册票据，开通生物支付免二次输密。
        if (clientInfo != null && PaySecurityConstants.CLIENT_PLATFORM_APP.equals(clientInfo.getPlatform())) {
            result.setBiometricEnrollmentToken(
                paySecurityRuntime.issueEnrollmentToken(user, security, orderNo, orderType, clientInfo));
        }
        return result;
    }

    /**
     * 订单绑定模式的二次确认授权，等价于金额参数为空的
     * {@link #authorizeLegacyConfirm(UserIdentity, String, String, BigDecimal, String, ClientInfo)}。
     */
    public AuthorizationResult authorizeLegacyConfirm(UserIdentity user, String orderNo, String orderType,
        ClientInfo clientInfo) {
        return authorizeLegacyConfirm(user, orderNo, orderType, null, null, clientInfo);
    }

    /**
     * 为尚未设置支付密码的兼容用户签发二次确认支付票据。
     *
     * <p>兼容通道：仅未设置支付密码的用户可用。票据按 securityVersion=1 快照签发，
     * 用户一旦首次设置密码（安全版本递增为2），历史二次确认票据立即失效。
     * 订单号为空时按声明金额+币种签发金额授权票据。</p>
     *
     * @param user 当前登录用户
     * @param orderNo 待支付订单号，可为空（金额授权模式）
     * @param orderType 订单类型
     * @param amount 订单号为空时必填的授权金额
     * @param currency 订单号为空时必填的币种
     * @param clientInfo 客户端环境快照
     * @return 支付授权结果
     */
    public AuthorizationResult authorizeLegacyConfirm(UserIdentity user, String orderNo, String orderType,
        BigDecimal amount, String currency, ClientInfo clientInfo) {
        paySecurityRuntime.ensureActiveUser(user);
        UserSecuritySettings security = paySecurityRuntime.getSettings(user.getUid());
        if (security != null && security.getPasswordStatus() == PayPasswordStatusEnum.ENABLED) {
            throw PaySecurityException.of(PaySecurityErrorCode.PAY_PASSWORD_ALREADY_SET);
        }
        AuthorizationResult result = paySecurityRuntime.issueAuthorization(user, security, orderNo, orderType, amount, currency,
            AuthorizeMethodEnum.LEGACY_CONFIRM, null, clientInfo);
        paySecurityRuntime.audit(AuditEventEnum.LEGACY_CONFIRM_AUTHORIZE, user.getUid(), orderNo, null,
            AuditResultEnum.SUCCESS, null, null, null, clientInfo);
        return result;
    }
}
