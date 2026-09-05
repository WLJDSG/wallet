package com.wallet.app.security;

import com.wallet.security.PaySecurityEngine;
import com.wallet.security.config.PaySecurityProperties;
import com.wallet.security.spi.AuditRecorder;
import com.wallet.security.spi.BiometricCredentialRepository;
import com.wallet.security.spi.OrderPaymentInfoResolver;
import com.wallet.security.spi.PaySecurityKeyValueStore;
import com.wallet.security.spi.SmsSender;
import com.wallet.security.spi.UserSecuritySettingsRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 钱包自有支付安全内核装配。 */
@Configuration
public class PaySecurityConfig {

    @Bean
    @ConfigurationProperties("wallet.pay-security")
    PaySecurityProperties paySecurityProperties() {
        PaySecurityProperties properties = new PaySecurityProperties();
        properties.setRedisKeyPrefix("wallet:security:");
        return properties;
    }

    /** 未接真实短信供应商时安全失败，且绝不把验证码输出到日志。业务方可提供同类型 Bean 替换。 */
    @Bean
    @ConditionalOnMissingBean(SmsSender.class)
    SmsSender unavailableSmsSender() {
        return (phone, code, ttlSeconds) -> false;
    }

    /** 未接入账号中心时不接受客户端自报手机号，由业务方提供同类型 Bean 替换。 */
    @Bean
    @ConditionalOnMissingBean(UserPhoneResolver.class)
    UserPhoneResolver unavailableUserPhoneResolver() {
        return userId -> null;
    }

    @Bean
    PaySecurityEngine paySecurityEngine(PaySecurityProperties properties, PaySecurityKeyValueStore keyValueStore,
        UserSecuritySettingsRepository settings, BiometricCredentialRepository credentials,
        AuditRecorder auditRecorder, SmsSender smsSender, OrderPaymentInfoResolver orderResolver) {
        return new PaySecurityEngine(properties, keyValueStore, settings, credentials, auditRecorder, smsSender, orderResolver);
    }
}
