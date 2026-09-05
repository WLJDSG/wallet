package com.wallet.security.testutil;

import com.wallet.security.PaySecurityEngine;
import com.wallet.security.config.PaySecurityProperties;
import com.wallet.security.enums.IdentityPurposeEnum;
import com.wallet.security.core.Hashes;
import com.wallet.security.core.RedisKeys;
import com.wallet.security.error.PaySecurityErrorCode;
import com.wallet.security.error.PaySecurityException;
import com.wallet.security.model.ClientInfo;
import com.wallet.security.model.UserIdentity;
import org.junit.jupiter.api.BeforeEach;

import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * 以内存替身组装 支付安全内核 的服务层测试基类。
 */
public abstract class PaySecurityEngineTestSupport {

    protected PaySecurityProperties properties;
    protected InMemoryKvStore kv;
    protected InMemoryUserSecuritySettingsRepository settings;
    protected InMemoryCredentialRepository credentials;
    protected RecordingAuditRecorder audits;
    protected StubSmsSender sms;
    protected StubOrderPaymentInfoResolver orders;
    protected PaySecurityEngine engine;
    protected RedisKeys keys;
    protected UserIdentity user;
    protected ClientInfo appClientInfo;

    @BeforeEach
    public void setUpEngine() {
        properties = new PaySecurityProperties();
        // 降低 BCrypt 强度以加速测试；默认强度由 PaySecurityPropertiesTest 单独断言
        properties.setBcryptStrength(4);
        kv = new InMemoryKvStore();
        settings = new InMemoryUserSecuritySettingsRepository();
        credentials = new InMemoryCredentialRepository();
        audits = new RecordingAuditRecorder();
        sms = new StubSmsSender();
        orders = new StubOrderPaymentInfoResolver();
        engine = new PaySecurityEngine(properties, kv, settings, credentials, audits, sms, orders);
        keys = new RedisKeys(properties.getRedisKeyPrefix(), ZoneId.of(properties.getBusinessZoneId()));
        user = UserIdentity.of(7L, "13800000000", true);
        appClientInfo = new ClientInfo("app", "ios", "100", "127.0.0.1", "junit-agent");
    }

    /** 用当前字段重建 支付安全内核；用例可先替换某个替身（如注入模拟并发的仓储）再调用。 */
    protected void rebuildEngine() {
        engine = new PaySecurityEngine(properties, kv, settings, credentials, audits, sms, orders);
    }

    /** 与 支付安全内核 内部一致的手机号频控键哈希（HMAC，密钥取当前 properties 配置）。 */
    protected String phoneHash() {
        return Hashes.hmacSha256(properties.getPhoneHashPepper(), user.getPhone());
    }

    /** 走完整短信身份流程设置支付密码，并清除发送冷却便于同一用例再次发送。 */
    protected void setPassword(String password) {
        engine.getIdentityService().sendIdentityCode(user, IdentityPurposeEnum.PASSWORD_SET);
        String identityToken = engine.getIdentityService().verifyIdentityCode(user, IdentityPurposeEnum.PASSWORD_SET, sms.lastCode, appClientInfo);
        engine.getIdentityService().updatePassword(user, IdentityPurposeEnum.PASSWORD_SET, identityToken, password, password, appClientInfo);
        kv.delete(keys.smsSendCooldown("PASSWORD_SET", phoneHash()));
    }

    /** 断言动作抛出指定错误码并返回异常供进一步断言。 */
    protected PaySecurityException expectError(PaySecurityErrorCode expected, Runnable action) {
        try {
            action.run();
        } catch (PaySecurityException e) {
            assertEquals(expected, e.getErrorCode());
            return e;
        }
        fail("期望抛出 " + expected + " 但未抛出任何异常");
        return null;
    }
}
