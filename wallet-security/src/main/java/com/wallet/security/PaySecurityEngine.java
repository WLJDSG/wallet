package com.wallet.security;

import com.wallet.security.config.PaySecurityProperties;
import com.wallet.security.service.AuthorizationService;
import com.wallet.security.service.BiometricService;
import com.wallet.security.service.IdentityService;
import com.wallet.security.service.PasswordAuthService;
import com.wallet.security.service.PaySecurityRuntime;
import com.wallet.security.spi.AuditRecorder;
import com.wallet.security.spi.BiometricCredentialRepository;
import com.wallet.security.spi.OrderPaymentInfoResolver;
import com.wallet.security.spi.PaySecurityKeyValueStore;
import com.wallet.security.spi.SmsSender;
import com.wallet.security.spi.UserSecuritySettingsRepository;
import lombok.Getter;

import java.util.Objects;

/**
 * 支付安全内核组装入口。
 *
 * <p>宿主提供 6 个 SPI 实现（KV 存储、2 个仓储、审计出口、短信发送、订单事实解析）
 * 和策略配置，构造后通过 4 个域服务使用全部能力：</p>
 *
 * <pre>{@code
 * PaySecurityEngine engine = new PaySecurityEngine(paySecurityProperties, paySecurityKeyValueStore, userSecuritySettingsRepository,
 *     biometricCredentialRepository, auditRecorder, smsSender, orderPaymentInfoResolver);
 * engine.getPasswordAuthService().authorizePassword(...);
 * }</pre>
 *
 * <p>Engine 无状态且线程安全，宿主应用内建一个单例即可。</p>
 */
@Getter
public final class PaySecurityEngine {

    /** 支付安全状态与密码/二次确认授权服务 */
    private final PasswordAuthService passwordAuthService;
    /** 短信身份验证与密码设置、修改、重置服务 */
    private final IdentityService identityService;
    /** 生物凭证注册、验签与生命周期管理服务 */
    private final BiometricService biometricService;
    /** 支付授权票据策略判定与一次性消费服务 */
    private final AuthorizationService authorizationService;

    public PaySecurityEngine(PaySecurityProperties paySecurityProperties, PaySecurityKeyValueStore paySecurityKeyValueStore,
        UserSecuritySettingsRepository userSecuritySettingsRepository, BiometricCredentialRepository biometricCredentialRepository,
        AuditRecorder auditRecorder, SmsSender smsSender, OrderPaymentInfoResolver orderPaymentInfoResolver) {
        PaySecurityRuntime paySecurityRuntime = new PaySecurityRuntime(
            Objects.requireNonNull(paySecurityProperties, "paySecurityProperties 不能为空"),
            Objects.requireNonNull(paySecurityKeyValueStore, "paySecurityKeyValueStore 未实现：请提供 PaySecurityKeyValueStore"),
            Objects.requireNonNull(userSecuritySettingsRepository, "userSecuritySettingsRepository 未实现：请提供 UserSecuritySettingsRepository"),
            Objects.requireNonNull(biometricCredentialRepository,
                "biometricCredentialRepository 未实现：请提供 BiometricCredentialRepository"),
            Objects.requireNonNull(auditRecorder, "auditRecorder 未实现：请提供 AuditRecorder"),
            Objects.requireNonNull(smsSender, "smsSender 未实现：请提供 SmsSender"),
            Objects.requireNonNull(orderPaymentInfoResolver,
                "orderPaymentInfoResolver 未实现：请提供 OrderPaymentInfoResolver"));
        this.passwordAuthService = new PasswordAuthService(paySecurityRuntime);
        this.identityService = new IdentityService(paySecurityRuntime);
        this.biometricService = new BiometricService(paySecurityRuntime);
        this.authorizationService = new AuthorizationService(paySecurityRuntime);
    }
}
