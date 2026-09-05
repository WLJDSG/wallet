package com.wallet.security.core;

import com.wallet.security.enums.LockoutScopeEnum;

import java.time.ZoneId;

/**
 * 支付安全全部 KV 键模板。默认前缀 "pay:" 下与既有生产键完全一致，
 * 任何模板变更都会使在途票据与计数失效，等同存储协议升级。
 */
public final class RedisKeys {

    private final String prefix;
    private final ZoneId businessZone;

    public RedisKeys(String prefix, ZoneId businessZone) {
        this.prefix = prefix;
        this.businessZone = businessZone;
    }

    /** 支付授权票据：{prefix}auth:{sha256(token)} */
    public String authorization(String tokenHash) {
        return prefix + "auth:" + tokenHash;
    }

    /** 支付授权票据消费占位：{prefix}auth:used:{sha256(token)} */
    public String authorizationUsed(String tokenHash) {
        return prefix + "auth:used:" + tokenHash;
    }

    /** 支付密码连续失败计数：{prefix}pwd:{scope-infix}fail:continuous:{uid}，余额支付作用域无中缀与既有生产键一致 */
    public String continuousFailure(LockoutScopeEnum scope, Long uid) {
        return prefix + "pwd:" + scope.getKeyInfix() + "fail:continuous:" + uid;
    }

    /** 支付密码当日失败计数：{prefix}pwd:{scope-infix}fail:daily:{yyyy-MM-dd}:{uid} */
    public String dailyFailure(LockoutScopeEnum scope, Long uid) {
        return prefix + "pwd:" + scope.getKeyInfix() + "fail:daily:" + BusinessTime.today(businessZone) + ":" + uid;
    }

    /** 作用域锁定（值为解锁毫秒时间戳）：{prefix}pwd:{scope-infix}lock:{uid} */
    public String lock(LockoutScopeEnum scope, Long uid) {
        return prefix + "pwd:" + scope.getKeyInfix() + "lock:" + uid;
    }

    /** 一次性身份票据：{prefix}identity:{sha256(token)} */
    public String identity(String tokenHash) {
        return prefix + "identity:" + tokenHash;
    }

    /** 身份票据消费占位：{prefix}identity:used:{sha256(token)} */
    public String identityUsed(String tokenHash) {
        return prefix + "identity:used:" + tokenHash;
    }

    /** 生物注册授权票据：{prefix}identity:BIOMETRIC_ENROLLMENT:{sha256(token)} */
    public String enrollment(String tokenHash) {
        return prefix + "identity:BIOMETRIC_ENROLLMENT:" + tokenHash;
    }

    /** 生物注册授权票据消费占位：{prefix}identity:BIOMETRIC_ENROLLMENT:used:{sha256(token)} */
    public String enrollmentUsed(String tokenHash) {
        return prefix + "identity:BIOMETRIC_ENROLLMENT:used:" + tokenHash;
    }

    /** 生物注册会话：{prefix}bio:registration:{registrationId} */
    public String registration(String registrationId) {
        return prefix + "bio:registration:" + registrationId;
    }

    /** 生物注册会话消费占位：{prefix}bio:registration:used:{registrationId} */
    public String registrationUsed(String registrationId) {
        return prefix + "bio:registration:used:" + registrationId;
    }

    /** 生物签名挑战：{prefix}bio:challenge:{challengeId} */
    public String challenge(String challengeId) {
        return prefix + "bio:challenge:" + challengeId;
    }

    /** 生物签名挑战消费占位：{prefix}bio:challenge:used:{challengeId} */
    public String challengeUsed(String challengeId) {
        return prefix + "bio:challenge:used:" + challengeId;
    }

    /** 支付安全短信验证码：{prefix}sms:{purpose}:{uid} */
    public String smsCode(String purpose, Long uid) {
        return prefix + "sms:" + purpose + ":" + uid;
    }

    /** 短信发送冷却：{prefix}sms:send:cooldown:{purpose}:{sha256(phone)} */
    public String smsSendCooldown(String purpose, String phoneHash) {
        return prefix + "sms:send:cooldown:" + purpose + ":" + phoneHash;
    }

    /** 短信每日发送计数：{prefix}sms:send:daily:{yyyy-MM-dd}:{purpose}:{sha256(phone)} */
    public String smsSendDaily(String purpose, String phoneHash) {
        return prefix + "sms:send:daily:" + BusinessTime.today(businessZone) + ":" + purpose + ":" + phoneHash;
    }

    /** 短信验证失败计数：{prefix}sms:verify:fail:{purpose}:{sha256(phone)} */
    public String smsVerifyFailure(String purpose, String phoneHash) {
        return prefix + "sms:verify:fail:" + purpose + ":" + phoneHash;
    }

    /** 短信验证锁定：{prefix}sms:verify:lock:{purpose}:{sha256(phone)} */
    public String smsVerifyLock(String purpose, String phoneHash) {
        return prefix + "sms:verify:lock:" + purpose + ":" + phoneHash;
    }
}
