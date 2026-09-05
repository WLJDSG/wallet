package com.wallet.security.service;

import com.wallet.security.core.Hashes;
import com.wallet.security.core.PaySecurityChecks;
import com.wallet.security.enums.AuditEventEnum;
import com.wallet.security.enums.AuditResultEnum;
import com.wallet.security.enums.CredentialDisabledReasonEnum;
import com.wallet.security.enums.IdentityPurposeEnum;
import com.wallet.security.enums.LockoutScopeEnum;
import com.wallet.security.enums.PasswordAuditEvent;
import com.wallet.security.enums.PayPasswordStatusEnum;
import com.wallet.security.error.PaySecurityErrorCode;
import com.wallet.security.error.PaySecurityException;
import com.wallet.security.model.ClientInfo;
import com.wallet.security.model.UserIdentity;
import com.wallet.security.spi.model.UserSecuritySettings;
import com.wallet.security.token.PayIdentityToken;

import java.util.Date;

/**
 * 支付密码的身份验证与设置、修改、重置。
 *
 * <p>短信验证码由 支付安全内核 生成并存储在自己的 KV 键下，宿主 SmsSender 只负责发送；
 * 身份票据一次性消费，用途不可互换。</p>
 */
public final class IdentityService {

    private final PaySecurityRuntime paySecurityRuntime;

    /** 内部构造器，宿主请通过 {@code PaySecurityEngine} 装配。 */
    public IdentityService(PaySecurityRuntime paySecurityRuntime) {
        this.paySecurityRuntime = paySecurityRuntime;
    }

    /**
     * 发送设置或重置支付密码使用的短信验证码。
     *
     * <p>流程节点：用途状态机校验（SET 须未设密码 / RESET 须已设）→ 冷却与每日上限
     * → 支付安全内核 生成6位安全随机码 → 先落存储后发送，发送失败立即回收验证码。</p>
     *
     * @param user 当前登录用户（取 uid 与手机号）
     * @param purpose 验证码用途：PASSWORD_SET 或 PASSWORD_RESET
     */
    public void sendIdentityCode(UserIdentity user, IdentityPurposeEnum purpose) {
        paySecurityRuntime.ensureActiveUser(user);
        paySecurityRuntime.validateSmsPurpose(user.getUid(), purpose);
        String phoneHash = paySecurityRuntime.phoneHash(user.getPhone());
        // 冷却占位与每日计数一经写入不回滚：即使后续发送失败，冷却期内也不允许再次触发发送。
        paySecurityRuntime.getSmsRateLimiter().ensureSendAllowed(purpose.name(), phoneHash);
        // 验证码由 支付安全内核 生成并先落存储再发送，发送失败立即回收，避免出现”已发出但服务端无码”的窗口。
        String code = paySecurityRuntime.generateSmsCode();
        long codeTtlSeconds = paySecurityRuntime.getPaySecurityProperties().getSmsCodeTtlSeconds();
        String codeKey = paySecurityRuntime.getRedisKeys().smsCode(purpose.name(), user.getUid());
        if (!paySecurityRuntime.getPaySecurityKeyValueStore().set(codeKey, code, codeTtlSeconds)) {
            throw PaySecurityException.of(PaySecurityErrorCode.PAY_SECURITY_UNAVAILABLE);
        }
        boolean sent;
        try {
            sent = paySecurityRuntime.getSmsSender().sendVerificationCode(user.getPhone(), code, codeTtlSeconds);
        } catch (RuntimeException e) {
            paySecurityRuntime.getPaySecurityKeyValueStore().delete(codeKey);
            throw e;
        }
        if (!sent) {
            paySecurityRuntime.getPaySecurityKeyValueStore().delete(codeKey);
            throw PaySecurityException.of(PaySecurityErrorCode.PAY_SECURITY_UNAVAILABLE);
        }
    }

    /**
     * 校验短信验证码并签发对应用途的一次性身份票据。
     *
     * <p>流程节点：用途状态机校验 → 验证锁定检查 → 常量时间比较 → 失败计数（达阈值删码并锁定）
     * / 成功销毁验证码并清计数 → 签发绑定当前安全版本快照的身份票据。</p>
     *
     * @param user 当前登录用户
     * @param purpose 验证码用途：PASSWORD_SET 或 PASSWORD_RESET
     * @param code 6位短信验证码
     * @param clientInfo 客户端环境快照
     * @return 一次性身份票据
     */
    public String verifyIdentityCode(UserIdentity user, IdentityPurposeEnum purpose, String code, ClientInfo clientInfo) {
        paySecurityRuntime.ensureActiveUser(user);
        paySecurityRuntime.validateSmsPurpose(user.getUid(), purpose);
        String phoneHash = paySecurityRuntime.phoneHash(user.getPhone());
        paySecurityRuntime.getSmsRateLimiter().ensureVerifyNotLocked(purpose.name(), phoneHash);
        // 常量时间比较验证码，避免逐位比较的时序差异泄露正确前缀。
        String smsCodeKey = paySecurityRuntime.getRedisKeys().smsCode(purpose.name(), user.getUid());
        String expected = paySecurityRuntime.getPaySecurityKeyValueStore().get(smsCodeKey);
        if (expected == null || !PaySecurityChecks.constantTimeEquals(expected, code)) {
            paySecurityRuntime.getSmsRateLimiter().recordVerifyFailure(purpose.name(), phoneHash, user.getUid());
            paySecurityRuntime.audit(AuditEventEnum.SMS_CODE_VERIFY, user.getUid(), null, null,
                AuditResultEnum.FAIL, PaySecurityErrorCode.PAY_IDENTITY_CODE_INVALID.getCode(), null, null,
                clientInfo);
            throw PaySecurityException.of(PaySecurityErrorCode.PAY_IDENTITY_CODE_INVALID);
        }
        // 一码一用：以删除成功作为原子消费凭证，并发提交同一验证码只有一个能通过。
        if (!paySecurityRuntime.getPaySecurityKeyValueStore().delete(smsCodeKey)) {
            throw PaySecurityException.of(PaySecurityErrorCode.PAY_IDENTITY_CODE_INVALID);
        }
        paySecurityRuntime.getSmsRateLimiter().clearVerifyFailures(purpose.name(), phoneHash);
        paySecurityRuntime.audit(AuditEventEnum.SMS_CODE_VERIFY, user.getUid(), null, null,
            AuditResultEnum.SUCCESS, null, null, null, clientInfo);
        UserSecuritySettings security = paySecurityRuntime.getSettings(user.getUid());
        return paySecurityRuntime.issueIdentityToken(user.getUid(), purpose, security, null, null, clientInfo);
    }

    /**
     * 校验当前支付密码并签发修改密码或注册生物凭证的身份票据。
     *
     * <p>流程节点：用途白名单（仅修改密码/开通生物）→ 密码已设校验 → 锁定检查
     * → BCrypt 校验（失败计数与锁定独立于余额支付作用域，连续超限仅锁定修改密码功能）
     * → 签发身份票据。</p>
     *
     * @param user 当前登录用户
     * @param purpose 身份票据用途：PASSWORD_CHANGE 或 BIOMETRIC_ENROLLMENT
     * @param password 当前6位支付密码
     * @param orderNo 来源订单号，支付成功页开启生物支付时传入
     * @param orderType 来源订单类型
     * @param clientInfo 客户端环境快照
     * @return 一次性身份票据
     */
    public String authorizePasswordIdentity(UserIdentity user, IdentityPurposeEnum purpose, String password,
        String orderNo, String orderType, ClientInfo clientInfo) {
        paySecurityRuntime.ensureActiveUser(user);
        if (purpose != IdentityPurposeEnum.PASSWORD_CHANGE
            && purpose != IdentityPurposeEnum.BIOMETRIC_ENROLLMENT) {
            throw PaySecurityException.of(PaySecurityErrorCode.PAY_IDENTITY_PURPOSE_INVALID);
        }
        UserSecuritySettings security = paySecurityRuntime.getSettings(user.getUid());
        if (security == null || security.getPasswordStatus() != PayPasswordStatusEnum.ENABLED) {
            throw PaySecurityException.of(PaySecurityErrorCode.PAY_PASSWORD_NOT_SET);
        }
        paySecurityRuntime.getLockoutManager().ensureNotLocked(LockoutScopeEnum.PASSWORD_CHANGE, user.getUid());
        if (!paySecurityRuntime.getBcryptHasher().matches(password, security.getPasswordHash())) {
            int remainingAttempts = paySecurityRuntime.getLockoutManager()
                .recordPasswordFailure(LockoutScopeEnum.PASSWORD_CHANGE, user.getUid());
            paySecurityRuntime.audit(AuditEventEnum.PASSWORD_IDENTITY_AUTHORIZE, user.getUid(), orderNo, null,
                AuditResultEnum.FAIL, PaySecurityErrorCode.PAY_PASSWORD_ERROR.getCode(), null, null, clientInfo);
            // 本次失败恰好触发锁定时，直接按锁定报错而非剩余次数。
            paySecurityRuntime.getLockoutManager().ensureNotLocked(LockoutScopeEnum.PASSWORD_CHANGE, user.getUid());
            throw PaySecurityException.of(PaySecurityErrorCode.PAY_PASSWORD_ERROR, remainingAttempts);
        }
        paySecurityRuntime.getLockoutManager().clearContinuousFailures(LockoutScopeEnum.PASSWORD_CHANGE, user.getUid());
        paySecurityRuntime.audit(AuditEventEnum.PASSWORD_IDENTITY_AUTHORIZE, user.getUid(), orderNo, null,
            AuditResultEnum.SUCCESS, null, null, null, clientInfo);
        return paySecurityRuntime.issueIdentityToken(user.getUid(), purpose, security, orderNo, orderType, clientInfo);
    }


    /**
     * 消费身份票据并设置、修改或重置支付密码。
     *
     * <p>宿主门面应在事务内调用本方法，保证档案更新与凭证停用的原子性。</p>
     *
     * <p>流程节点：新密码规则校验 → 票据核对（uid/用途匹配）→ 安全版本快照核对（防并发状态漂移）
     * → 密码状态互斥校验 → 原子占用票据（一次性）→ 档案写入并递增版本
     * → 停用全部旧凭证（非首次设置）→ 清空锁定与计数 → 销毁票据 → 审计。</p>
     *
     * @param user 当前登录用户
     * @param purpose 本次操作用途：PASSWORD_SET、PASSWORD_CHANGE 或 PASSWORD_RESET
     * @param identityToken 与用途绑定的一次性身份票据
     * @param password 新6位支付密码
     * @param confirmPassword 确认新支付密码
     * @param clientInfo 客户端环境快照
     */
    public void updatePassword(UserIdentity user, IdentityPurposeEnum purpose, String identityToken, String password,
        String confirmPassword, ClientInfo clientInfo) {
        paySecurityRuntime.ensureActiveUser(user);
        if (!password.equals(confirmPassword) || !PaySecurityChecks.isValidPassword(password)) {
            throw PaySecurityException.of(PaySecurityErrorCode.PAY_PASSWORD_INVALID);
        }
        String tokenHash = Hashes.sha256(identityToken);
        String identityKey = paySecurityRuntime.getRedisKeys().identity(tokenHash);
        PayIdentityToken token = paySecurityRuntime.getJsonHelper()
            .fromJson(paySecurityRuntime.getPaySecurityKeyValueStore().get(identityKey), PayIdentityToken.class);
        // 票据必须与当前用户、当前操作用途完全匹配，短信重置票据不能挪用于修改密码。
        if (token == null || !user.getUid().equals(token.getUid()) || purpose != token.getPurpose()) {
            throw PaySecurityException.of(PaySecurityErrorCode.PAY_IDENTITY_TOKEN_INVALID);
        }
        // 版本快照核对：票据签发后档案被并发变更（如撤销全部凭证）时立即作废。
        UserSecuritySettings security = paySecurityRuntime.getSettings(user.getUid());
        int currentSecurityVersion = security == null ? 1 : security.getSecurityVersion();
        if (!Integer.valueOf(currentSecurityVersion).equals(token.getSecurityVersion())) {
            throw PaySecurityException.of(PaySecurityErrorCode.PAY_SECURITY_STATE_CHANGED);
        }
        // 状态互斥：首次设置要求当前未设密码，修改/重置要求已设密码。
        boolean passwordSet = security != null && security.getPasswordStatus() == PayPasswordStatusEnum.ENABLED;
        if ((purpose == IdentityPurposeEnum.PASSWORD_SET && passwordSet)
            || (purpose != IdentityPurposeEnum.PASSWORD_SET && !passwordSet)) {
            throw PaySecurityException.of(PaySecurityErrorCode.PAY_SECURITY_STATE_CHANGED);
        }
        // 原子占用消费标记，并发提交只有一个能进入写库。
        String identityUsedKey = paySecurityRuntime.getRedisKeys().identityUsed(tokenHash);
        if (!paySecurityRuntime.getPaySecurityKeyValueStore().setIfAbsent(identityUsedKey, "1",
            paySecurityRuntime.authorizationTtlSeconds())) {
            throw PaySecurityException.of(PaySecurityErrorCode.PAY_IDENTITY_TOKEN_USED);
        }
        Date now = new Date();
        if (security == null) {
            // 未设密码状态按 securityVersion=1 签发的直付票据，首次设置必须递增为2使其立即失效。
            security = new UserSecuritySettings();
            security.setUid(user.getUid());
            security.setPasswordVersion(1);
            security.setSecurityVersion(2);
            security.setPasswordHash(paySecurityRuntime.getBcryptHasher().encode(password));
            security.setPasswordStatus(PayPasswordStatusEnum.ENABLED);
            security.setPasswordSetAt(now);
            security.setPasswordUpdatedAt(now);
            paySecurityRuntime.getUserSecuritySettingsRepository().insert(security);
        } else {
            int expectedSecurityVersion = security.getSecurityVersion();
            security.setPasswordVersion(security.getPasswordVersion() + 1);
            security.setSecurityVersion(expectedSecurityVersion + 1);
            security.setPasswordHash(paySecurityRuntime.getBcryptHasher().encode(password));
            security.setPasswordStatus(PayPasswordStatusEnum.ENABLED);
            security.setPasswordUpdatedAt(now);
            if (security.getPasswordSetAt() == null) {
                security.setPasswordSetAt(now);
            }
            // 版本 CAS：读取与写入之间档案被并发变更（另一次改密、全量撤销）时拒绝写入，
            // 防止旧快照回写覆盖新密码哈希或吞掉并发的版本递增。
            if (!paySecurityRuntime.getUserSecuritySettingsRepository().updateWithVersion(security, expectedSecurityVersion)) {
                throw PaySecurityException.of(PaySecurityErrorCode.PAY_SECURITY_STATE_CHANGED);
            }
        }
        if (purpose != IdentityPurposeEnum.PASSWORD_SET) {
            // 修改或重置密码后停用全部旧公钥，避免旧设备继续使用历史密码版本授权。
            paySecurityRuntime.disableAllCredentials(user.getUid(), CredentialDisabledReasonEnum.PASSWORD_CHANGED);
        }
        // 新密码生效即解除余额支付锁定并清空全部失败计数（重置密码是被锁定用户的自救通道）。
        paySecurityRuntime.getLockoutManager().clearPasswordFailureState(user.getUid());
        paySecurityRuntime.getPaySecurityKeyValueStore().delete(identityKey);
        paySecurityRuntime.audit(passwordAuditEventFor(purpose), user.getUid(), null, null, AuditResultEnum.SUCCESS,
            null, null, null, clientInfo);
    }

    /** 把身份票据用途映射为审计事件类型；只有 {@code PASSWORD_*} 三种用途落审计，BIOMETRIC_ENROLLMENT 不落。 */
    private static PasswordAuditEvent passwordAuditEventFor(IdentityPurposeEnum purpose) {
        switch (purpose) {
            case PASSWORD_SET:
                return PasswordAuditEvent.PASSWORD_SET;
            case PASSWORD_CHANGE:
                return PasswordAuditEvent.PASSWORD_CHANGE;
            case PASSWORD_RESET:
                return PasswordAuditEvent.PASSWORD_RESET;
            default:
                throw new IllegalArgumentException("非密码用途不应作为审计事件落库：" + purpose);
        }
    }
}
