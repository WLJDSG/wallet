package com.wallet.security.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 支付安全审计事件结果。
 *
 * <p>取值随 pay_security_audit.result 持久化。</p>
 */
@Getter
@AllArgsConstructor
public enum AuditResultEnum {

    /** 事件成功。 */
    SUCCESS("成功"),

    /** 事件失败，失败原因见 reason_code。 */
    FAIL("失败"),

    ;

    /** 结果说明。 */
    private final String description;

}
