package com.wallet.security.service;

import com.wallet.security.core.Hashes;
import com.wallet.security.core.PaySecurityChecks;
import com.wallet.security.enums.AuditEventEnum;
import com.wallet.security.enums.AuditResultEnum;
import com.wallet.security.enums.AuthorizeMethodEnum;
import com.wallet.security.enums.LockoutScopeEnum;
import com.wallet.security.enums.PayPasswordStatusEnum;
import com.wallet.security.error.PaySecurityErrorCode;
import com.wallet.security.error.PaySecurityException;
import com.wallet.security.model.AuthorizationConsumeCommand;
import com.wallet.security.model.ClientInfo;
import com.wallet.security.model.OrderPaymentInfo;
import com.wallet.security.model.UserIdentity;
import com.wallet.security.spi.model.UserSecuritySettings;
import com.wallet.security.token.PayAuthorizationToken;
import com.wallet.security.core.Texts;

/**
 * 支付授权票据的策略判定与一次性消费。
 *
 * <p>{@link #consume} 是余额支付的最终安全门：核对用户、订单、金额、币种、
 * 密码/安全版本、锁定状态和生物凭证状态，校验通过后原子占用票据，
 * 并发请求不能重复进入扣款。</p>
 *
 * <p>票据生命周期：授权成功签发（TTL 300 秒，原文只回传客户端、存储只落 sha256）
 * → 客户端支付时随请求携带 → consume 原子占用并逐项复核 → 成功即销毁；复核失败释放占用允许重试。</p>
 */
public final class AuthorizationService {

    private final PaySecurityRuntime paySecurityRuntime;

    /** 内部构造器，宿主请通过 {@code PaySecurityEngine} 装配。 */
    public AuthorizationService(PaySecurityRuntime paySecurityRuntime) {
        this.paySecurityRuntime = paySecurityRuntime;
    }

    /**
     * 判断本次余额支付是否必须提供支付授权票据。
     *
     * <p>“是否余额支付、实付金额是否大于 0”属于宿主业务口径，应由宿主在调用前判断。
     * 本方法的裁决口径：</p>
     *
     * <ol>
     * <li>总开关关闭直接放行（故障止损通道）；</li>
     * <li>渠道缺失/版本非法必须报错而不是降级放行；</li>
     * <li><b>已设支付密码的用户一律强制授权</b>——以服务端档案状态为准，
     *     不随客户端上报的版本号（可伪造）降级，低版本客户端只能引导升级；</li>
     * <li>未设密码的存量用户按版本边界裁决，边界之前维持旧流程直付。</li>
     * </ol>
     *
     * @param user 当前登录用户，可为 null（视同未设密码，仅按版本边界裁决）
     * @param clientInfo 客户端环境快照
     * @return true 表示必须提供支付授权票据
     */
    public boolean isAuthorizationRequired(UserIdentity user, ClientInfo clientInfo) {
        if (!paySecurityRuntime.getPaySecurityProperties().isEnabled()) {
            return false;
        }
        String platform = clientInfo == null ? null : clientInfo.getPlatform();
        if (Texts.isBlank(platform)) {
            throw PaySecurityException.of(PaySecurityErrorCode.PAY_SECURITY_CLIENT_INVALID);
        }
        boolean enforcedByVersion;
        try {
            enforcedByVersion = PaySecurityChecks.shouldEnforce(paySecurityRuntime.getPaySecurityProperties().getEnforceVersion(),
                clientInfo.getAppVersion());
        } catch (IllegalArgumentException e) {
            throw PaySecurityException.of(PaySecurityErrorCode.PAY_SECURITY_CLIENT_INVALID);
        }
        if (enforcedByVersion) {
            return true;
        }
        // 版本校验合法但未达强制边界：已设密码用户仍强制授权，堵住谎报低版本绕过密码的口子。
        UserSecuritySettings security = user == null ? null : paySecurityRuntime.getSettings(user.getUid());
        return security != null && security.getPasswordStatus() == PayPasswordStatusEnum.ENABLED;
    }

    /**
     * 在余额扣减前重新校验并一次性消费支付授权票据。
     *
     * <p>流程节点：非强制且未携带票据直接放行（灰度期兼容旧客户端）→ 强制必须携带票据
     * → 按 sha256 定位票据并区分“已使用/已过期” → 原子占用消费标记（并发扣款只有一个能通过）
     * → 逐项复核（见 {@link #verifyAuthorization}）→ 成功销毁票据并审计；复核失败释放占用允许合法重试。</p>
     *
     * @param command 消费命令（required、票据、用户、订单、金额、币种、环境）
     */
    public void consume(AuthorizationConsumeCommand command) {
        boolean required = command.isRequired();
        String rawToken = command.getPayAuthorizationToken();
        // 服务端未强制且客户端未带票据：按旧流程放行；只要客户端带了票据就必须走完整校验，不允许半途降级。
        if (!required && Texts.isBlank(rawToken)) {
            return;
        }
        if (Texts.isBlank(rawToken)) {
            throw PaySecurityException.of(PaySecurityErrorCode.PAY_AUTH_TOKEN_REQUIRED);
        }
        // 票据原文不落存储：按 sha256 定位，泄露存储也拿不到可用票据。
        String tokenHash = Hashes.sha256(rawToken);
        String authorizationKey = paySecurityRuntime.getRedisKeys().authorization(tokenHash);
        String authorizationUsedKey = paySecurityRuntime.getRedisKeys().authorizationUsed(tokenHash);
        PayAuthorizationToken authorization = paySecurityRuntime.getJsonHelper()
            .fromJson(paySecurityRuntime.getPaySecurityKeyValueStore().get(authorizationKey), PayAuthorizationToken.class);
        // 票据不存在时区分两种终态：已被消费（提示查询支付结果）与已过期（提示重新授权）。
        if (authorization == null) {
            if (paySecurityRuntime.getPaySecurityKeyValueStore().get(authorizationUsedKey) != null) {
                throw PaySecurityException.of(PaySecurityErrorCode.PAY_AUTH_TOKEN_USED);
            }
            throw PaySecurityException.of(PaySecurityErrorCode.PAY_AUTH_TOKEN_EXPIRED);
        }
        // 原子占用 used 标记保证并发请求只有一个能进入后续余额扣减；校验失败时释放以允许合法重试。
        if (!paySecurityRuntime.getPaySecurityKeyValueStore().setIfAbsent(authorizationUsedKey, "1",
            paySecurityRuntime.authorizationTtlSeconds())) {
            throw PaySecurityException.of(PaySecurityErrorCode.PAY_AUTH_TOKEN_USED);
        }
        try {
            verifyAuthorization(authorization, command);
            paySecurityRuntime.getPaySecurityKeyValueStore().delete(authorizationKey);
            paySecurityRuntime.audit(AuditEventEnum.PAY_AUTH_CONSUME, authorization.getUid(),
                authorization.getOrderNo(), authorization.getCredentialId(), AuditResultEnum.SUCCESS,
                authorization.getAuthorizationMethod(), authorization.getAmount(), authorization.getCurrency(),
                command.getClientInfo());
        } catch (RuntimeException e) {
            paySecurityRuntime.getPaySecurityKeyValueStore().delete(authorizationUsedKey);
            throw e;
        }
    }

    /**
     * 消费前逐项复核清单：票据未过期 → 用户存在/激活/归属 → 金额、币种与请求一致
     * →（票据带订单时）订单号、类型与请求一致 → 服务端实时复读订单（未支付、金额币种未漂移，
     * 金额授权票据也必须与实际订单金额一致）→ 按授权方式核对密码版本（PASSWORD）或
     * 密码仍未设置（LEGACY_CONFIRM）→ 全局安全版本一致 → 生物凭证仍可用（BIOMETRIC）→ 最终锁定检查。
     */
    private void verifyAuthorization(PayAuthorizationToken authorization, AuthorizationConsumeCommand command) {
        UserIdentity user = command.getUser();
        if (authorization.getExpiresAt() == null || authorization.getExpiresAt() < System.currentTimeMillis()) {
            throw PaySecurityException.of(PaySecurityErrorCode.PAY_AUTH_TOKEN_EXPIRED);
        }
        if (user == null || !user.isActive() || !authorization.getUid().equals(user.getUid())
            || authorization.getAmount().compareTo(command.getPayPrice()) != 0
            || !authorization.getCurrency().equals(command.getCurrency())) {
            throw PaySecurityException.of(PaySecurityErrorCode.PAY_AUTH_TOKEN_INVALID);
        }
        // 订单绑定票据必须订单号、类型完全一致；金额授权票据（orderNo 为空）只放松订单号绑定，
        // 金额、币种、归属与未支付复核不放松。
        if (authorization.getOrderNo() != null
            && (!authorization.getOrderNo().equals(command.getOrderNo())
                || !authorization.getOrderType().equals(command.getOrderType()))) {
            throw PaySecurityException.of(PaySecurityErrorCode.PAY_AUTH_TOKEN_INVALID);
        }
        // 不信任票据签发时的订单快照，消费前重新读取订单归属、支付状态、金额和币种。
        OrderPaymentInfo currentOrder = paySecurityRuntime.resolveOrder(user.getUid(), command.getOrderNo(), command.getOrderType());
        if (Boolean.TRUE.equals(currentOrder.getPaid())
            || currentOrder.getAmount().compareTo(authorization.getAmount()) != 0
            || !currentOrder.getCurrency().equals(authorization.getCurrency())) {
            throw PaySecurityException.of(PaySecurityErrorCode.PAY_SECURITY_STATE_CHANGED);
        }
        UserSecuritySettings security = paySecurityRuntime.getSettings(user.getUid());
        if (authorization.getAuthorizationMethod() == AuthorizeMethodEnum.PASSWORD) {
            if (security == null || security.getPasswordStatus() != PayPasswordStatusEnum.ENABLED
                || !authorization.getPasswordVersion().equals(security.getPasswordVersion())) {
                throw PaySecurityException.of(PaySecurityErrorCode.PAY_SECURITY_STATE_CHANGED);
            }
        }
        if (authorization.getAuthorizationMethod() == AuthorizeMethodEnum.LEGACY_CONFIRM
            && security != null && security.getPasswordStatus() == PayPasswordStatusEnum.ENABLED) {
            throw PaySecurityException.of(PaySecurityErrorCode.PAY_SECURITY_STATE_CHANGED);
        }
        // securityVersion 统一使密码变化、凭证全撤销等状态变更立即失效所有历史票据。
        int currentSecurityVersion = security == null ? 1 : security.getSecurityVersion();
        if (authorization.getSecurityVersion() == null
            || authorization.getSecurityVersion() != currentSecurityVersion) {
            throw PaySecurityException.of(PaySecurityErrorCode.PAY_SECURITY_STATE_CHANGED);
        }
        if (authorization.getAuthorizationMethod() == AuthorizeMethodEnum.BIOMETRIC) {
            paySecurityRuntime.getEnabledCredential(user.getUid(), authorization.getCredentialId());
        }
        paySecurityRuntime.getLockoutManager().ensureNotLocked(LockoutScopeEnum.BALANCE_PAY, user.getUid());
    }
}
