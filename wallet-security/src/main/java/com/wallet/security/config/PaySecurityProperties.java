package com.wallet.security.config;

import lombok.Data;

/**
 * 支付安全策略阈值。
 *
 * <p>默认值即当前生产基线，宿主应在装配 Kernel 前从自己的配置中心绑定覆盖
 * （Spring 宿主可在 @Bean 方法上加 @ConfigurationProperties，本类保持纯 POJO）。
 * 这些阈值是安全基线，应与 Redis 票据 TTL、密码锁定策略和短信频控保持一致；
 * 版本边界只能由服务端发布配置决定，不允许根据请求是否携带授权票据动态降级。</p>
 */
@Data
public class PaySecurityProperties {

    /** 支付安全能力总开关。 */
    private boolean enabled = true;

    /**
     * 本商城四端共用的首个强制支付授权客户端版本，-1 表示暂不强制。
     *
     * <p>各宿主商城（大陆、港澳台、海外）的客户端版本号体系互相独立，
     * 该边界没有跨商城通用的默认值，必须由各自宿主通过配置指定；
     * 支付安全内核 的默认值 -1 仅表示"未配置即不按版本强制"。</p>
     */
    private int enforceVersion = -1;

    /** 支付授权票据有效秒数。 */
    private long authorizationTtlSeconds = 300L;

    /** 生物签名挑战有效秒数。 */
    private long challengeTtlSeconds = 120L;

    /** 支付密码连续失败上限。 */
    private int passwordContinuousFailureLimit = 5;

    /** 支付密码当日累计失败上限。 */
    private int passwordDailyFailureLimit = 10;

    /** 余额支付场景连续失败达到上限后的锁定分钟数。 */
    private int passwordLockMinutes = 10;

    /** 修改支付密码场景连续失败达到上限后的锁定分钟数。 */
    private int passwordChangeLockMinutes = 30;

    /** 支付安全短信发送冷却秒数。 */
    private long smsCooldownSeconds = 60L;

    /** 支付安全短信每日发送上限。 */
    private int smsDailyLimit = 5;

    /** 支付安全短信验证码有效秒数。 */
    private long smsCodeTtlSeconds = 300L;

    /** 支付安全短信连续验证失败上限。 */
    private int smsVerifyFailureLimit = 5;

    /** 短信验证失败计数窗口秒数。 */
    private long smsVerifyFailureWindowSeconds = 600L;

    /** 短信验证失败达到上限后的锁定秒数。 */
    private long smsVerifyLockSeconds = 600L;

    /** BCrypt 慢哈希强度。 */
    private int bcryptStrength = 12;

    /** 每日计数与锁定截止使用的业务时区。 */
    private String businessZoneId = "Asia/Shanghai";

    /** Redis key 前缀。 */
    private String redisKeyPrefix = "pay:";

    /**
     * 手机号频控键的 HMAC-SHA256 密钥。手机号空间小，无盐哈希可被彩虹表/枚举还原，
     * 宿主应覆盖为私有密钥；密钥变更会使在途的短信冷却与失败计数键失效（可接受）。
     */
    private String phoneHashPepper = "pay-security-phone";
}
