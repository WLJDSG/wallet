package com.wallet.security.error;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 支付安全领域错误码。
 *
 * <p>枚举名即错误码字符串，与宿主 i18n 资源键一一对应。支付安全内核 不渲染文案，
 * 宿主应捕获 {@link PaySecurityException} 并按 {@link #getCode()} 渲染本地化消息。</p>
 *
 * <p>{@link #getZhCn()}/{@link #getZhTw()} 携带宿主 i18n 文案（与
 * messages_zh_CN.properties / messages_zh_TW.properties 逐字一致），
 * 供按用户报错文案 grep 定位错误码与抛错位置；一致性由宿主侧
 * PaySecurityI18nTest 强制校验。</p>
 */
@Getter
@AllArgsConstructor
public enum PaySecurityErrorCode {

    /** 支付密码未设置。 */
    PAY_PASSWORD_NOT_SET("请先设置支付密码", "請先設定支付密碼"),

    /** 支付密码已设置，不能重复初始化。 */
    PAY_PASSWORD_ALREADY_SET("支付密码已设置，请使用修改或重置功能", "支付密碼已設定，請使用修改或重設功能"),

    /** 支付密码错误，异常 data 携带剩余可尝试次数。 */
    PAY_PASSWORD_ERROR("支付密码错误，请重新输入", "支付密碼錯誤，請重新輸入"),

    /** 新支付密码不符合 6 位数字或弱口令规则。 */
    PAY_PASSWORD_INVALID("请输入符合规则的6位数字支付密码", "請輸入符合規則的6位數字支付密碼"),

    /** 密码错误次数过多，余额支付已锁定。 */
    PAY_SECURITY_LOCKED("支付密码错误次数过多，余额支付已锁定，请稍后重试或重置支付密码", "支付密碼錯誤次數過多，餘額支付已鎖定，請稍後重試或重設支付密碼"),

    /** 密码错误次数过多，修改支付密码已锁定。 */
    PAY_PASSWORD_CHANGE_LOCKED("支付密码错误次数过多，修改支付密码已锁定，请稍后重试或重置支付密码", "支付密碼錯誤次數過多，修改支付密碼已鎖定，請稍後重試或重設支付密碼"),

    /** 支付安全状态（版本、密码状态、订单快照）已变化，需重新授权。 */
    PAY_SECURITY_STATE_CHANGED("支付安全状态已变化，请重新获取支付状态并授权", "支付安全狀態已變更，請重新取得支付狀態並授權"),

    /** 支付安全底层存储或短信通道暂不可用。 */
    PAY_SECURITY_UNAVAILABLE("支付安全服务暂时不可用，请稍后重试", "支付安全服務暫時無法使用，請稍後重試"),

    /** 客户端渠道或版本信息无效。 */
    PAY_SECURITY_CLIENT_INVALID("客户端渠道或版本信息无效，请升级客户端后重试", "用戶端渠道或版本資訊無效，請升級用戶端後重試"),

    /** 订单参数不完整。 */
    PAY_SECURITY_ORDER_INVALID("订单信息不完整，无法进行支付安全校验", "訂單資訊不完整，無法進行支付安全校驗"),

    /** 金额授权参数无效（未传订单号时金额或币种缺失/非法）。 */
    PAY_SECURITY_AMOUNT_INVALID("支付授权金额参数无效，请重新发起支付", "支付授權金額參數無效，請重新發起支付"),

    /** 当前登录用户无效或已被禁用。 */
    PAY_SECURITY_USER_INVALID("当前账号状态异常，无法进行支付安全操作", "當前帳號狀態異常，無法進行支付安全操作"),

    /** 订单类型不支持安全余额支付。 */
    PAY_SECURITY_ORDER_TYPE_UNSUPPORTED("当前订单类型暂不支持安全余额支付", "當前訂單類型暫不支援安全餘額支付"),

    /** 本次余额支付必须携带支付授权票据。 */
    PAY_AUTH_TOKEN_REQUIRED("该账号已设置了余额支付密码，请更新版本后再试", "該帳號已設定了餘額支付密碼，請更新版本後再試"),

    /** 支付授权票据已过期。 */
    PAY_AUTH_TOKEN_EXPIRED("支付授权已过期，请重新授权", "支付授權已過期，請重新授權"),

    /** 支付授权票据已被消费。 */
    PAY_AUTH_TOKEN_USED("支付授权已使用，请查询订单支付结果", "支付授權已使用，請查詢訂單支付結果"),

    /** 支付授权票据与当前用户或订单不匹配。 */
    PAY_AUTH_TOKEN_INVALID("支付授权与当前订单不匹配，请重新授权", "支付授權與當前訂單不匹配，請重新授權"),

    /** 身份验证用途无效。 */
    PAY_IDENTITY_PURPOSE_INVALID("当前身份验证用途无效，请重新操作", "當前身份驗證用途無效，請重新操作"),

    /** 短信验证码错误或已失效。 */
    PAY_IDENTITY_CODE_INVALID("验证码错误或已失效，请重新输入", "驗證碼錯誤或已失效，請重新輸入"),

    /** 短信验证码错误次数过多已锁定。 */
    PAY_IDENTITY_CODE_LOCKED("验证码错误次数过多，请稍后重新获取", "驗證碼錯誤次數過多，請稍後重新取得"),

    /** 一次性身份票据无效。 */
    PAY_IDENTITY_TOKEN_INVALID("身份验证已失效，请重新验证", "身份驗證已失效，請重新驗證"),

    /** 一次性身份票据已被消费。 */
    PAY_IDENTITY_TOKEN_USED("身份验证已使用，请勿重复提交", "身份驗證已使用，請勿重複提交"),

    /** 验证码发送过于频繁。 */
    PAY_SMS_TOO_FREQUENT("验证码发送过于频繁，请稍后重试", "驗證碼發送過於頻繁，請稍後重試"),

    /** 验证码当日发送次数已达上限。 */
    PAY_SMS_DAILY_LIMIT("今日验证码发送次数已达上限，请明日再试", "今日驗證碼發送次數已達上限，請明日再試"),

    /** 生物注册来源订单尚未支付成功。 */
    BIOMETRIC_ENROLLMENT_ORDER_UNPAID("来源订单尚未支付成功，暂不能开启生物支付", "來源訂單尚未支付成功，暫不能開啟生物支付"),

    /** 生物注册票据或注册会话无效。 */
    BIOMETRIC_REGISTRATION_INVALID("生物支付注册信息无效，请重新验证支付密码后绑定", "生物支付註冊資訊無效，請重新驗證支付密碼後綁定"),

    /** 生物注册会话已被消费。 */
    BIOMETRIC_REGISTRATION_USED("生物支付注册会话已使用，请勿重复提交", "生物支付註冊會話已使用，請勿重複提交"),

    /** 生物支付凭证无效或与当前安全版本不一致。 */
    BIOMETRIC_CREDENTIAL_INVALID("生物支付凭证无效，请使用支付密码并重新绑定", "生物支付憑證無效，請使用支付密碼並重新綁定"),

    /** 生物签名挑战无效或已过期。 */
    BIOMETRIC_CHALLENGE_INVALID("生物支付验证已失效，请重新发起", "生物支付驗證已失效，請重新發起"),

    /** 生物签名挑战已被消费。 */
    BIOMETRIC_CHALLENGE_USED("生物支付验证已使用，请查询订单支付结果", "生物支付驗證已使用，請查詢訂單支付結果"),

    /** 生物签名验签失败。 */
    BIOMETRIC_SIGNATURE_INVALID("生物支付签名验证失败，请改用支付密码", "生物支付簽名驗證失敗，請改用支付密碼"),

    /** 订单不存在或不属于当前用户。 */
    ORDER_DOES_NOT_EXIST("订单不存在", "訂單不存在"),

    /** 订单已支付。 */
    ORDER_PAID("订单已支付", "訂單已付款");

    /** 简体中文用户文案，与宿主 messages_zh_CN.properties 逐字一致。 */
    private final String zhCn;

    /** 繁体中文用户文案，与宿主 messages_zh_TW.properties 逐字一致。 */
    private final String zhTw;

    /**
     * @return 与宿主 i18n 资源键一致的错误码字符串
     */
    public String getCode() {
        return name();
    }
}
