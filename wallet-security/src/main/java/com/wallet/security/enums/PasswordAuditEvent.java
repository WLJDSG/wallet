package com.wallet.security.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 审计表中作为事件类型持久化的密码生命周期事件。
 *
 * <p>取值随 pay_security_audit.event_type 持久化，属于跨端协议的一部分。
 * 与 {@link IdentityPurposeEnum} 不同：本枚举仅描述"作为审计事件落地"的密码操作，
 * 因此不包含 {@link IdentityPurposeEnum#BIOMETRIC_ENROLLMENT}（该票据用途不直接落审计表）。</p>
 *
 * <p>仅作为 {@link AuditEventEnum} 联合类型的事件来源，不替代
 * {@link IdentityPurposeEnum} 在票据签发/消费处的语义。</p>
 */
@Getter
@AllArgsConstructor
public enum PasswordAuditEvent implements ProtocolValue {

    /** 首次设置支付密码（短信验证码证明身份）。 */
    PASSWORD_SET("首次设置支付密码"),

    /** 修改支付密码（当前支付密码证明身份）。 */
    PASSWORD_CHANGE("修改支付密码"),

    /** 重置支付密码（短信验证码证明身份）。 */
    PASSWORD_RESET("重置支付密码"),

    ;

    /** 事件说明。 */
    private final String description;

}
