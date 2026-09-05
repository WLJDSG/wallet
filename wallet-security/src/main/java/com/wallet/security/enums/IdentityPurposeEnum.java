package com.wallet.security.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 身份票据用途。
 *
 * <p>票据签发时绑定用途，消费时必须完全匹配，不同用途的票据不可互换；
 * 取值随票据与审计记录持久化，属于跨端协议的一部分。
 * 其中密码类用途在审计表 event_type 中由 {@link PasswordAuditEvent} 落库，
 * 票据与审计事件的语义不能互换：本枚举管理"票据用途"，
 * {@link PasswordAuditEvent} 管理"审计事件类型"。</p>
 */
@Getter
@AllArgsConstructor
public enum IdentityPurposeEnum {

    /** 首次设置支付密码（短信验证码证明身份）。 */
    PASSWORD_SET("首次设置支付密码"),

    /** 修改支付密码（当前支付密码证明身份）。 */
    PASSWORD_CHANGE("修改支付密码"),

    /** 重置支付密码（短信验证码证明身份）。 */
    PASSWORD_RESET("重置支付密码"),

    /** 注册生物凭证（当前支付密码证明身份）。 */
    BIOMETRIC_ENROLLMENT("注册生物凭证"),

    ;

    /** 用途说明。 */
    private final String description;

}
