package com.wallet.security.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 支付安全审计事件类型。
 *
 * <p>取值随 pay_security_audit.event_type 持久化。除本枚举列出的事件外，
 * 密码设置、修改、重置成功时以 {@link PasswordAuditEvent} 的
 * PASSWORD_SET/PASSWORD_CHANGE/PASSWORD_RESET 作为事件类型落库，
 * 消费审计数据时两个取值集合都要覆盖。</p>
 */
@Getter
@AllArgsConstructor
public enum AuditEventEnum implements ProtocolValue {

    /** 支付密码授权。 */
    PASSWORD_AUTHORIZE("支付密码授权"),

    /** 支付密码身份验证（修改密码/开通生物前的旧密码校验）。 */
    PASSWORD_IDENTITY_AUTHORIZE("支付密码身份验证"),

    /** 短信验证码校验。 */
    SMS_CODE_VERIFY("短信验证码校验"),

    /** 未设密码二次确认授权。 */
    LEGACY_CONFIRM_AUTHORIZE("未设密码二次确认授权"),

    /** 注册生物凭证。 */
    BIOMETRIC_CREDENTIAL_REGISTER("注册生物凭证"),

    /** 生物签名授权。 */
    BIOMETRIC_AUTHORIZE("生物签名授权"),

    /** 解绑单个生物凭证。 */
    BIOMETRIC_CREDENTIAL_REVOKE("解绑生物凭证"),

    /** 查询凭证状态失败。 */
    BIOMETRIC_CREDENTIAL_STATUS("查询凭证状态失败"),

    /** 全量撤销生物凭证。 */
    BIOMETRIC_CREDENTIAL_REVOKE_ALL("全量撤销生物凭证"),

    /** 签发支付授权票据。 */
    PAY_AUTH_ISSUE("签发支付授权票据"),

    /** 消费支付授权票据。 */
    PAY_AUTH_CONSUME("消费支付授权票据"),

    ;

    /** 事件说明。 */
    private final String description;

}
