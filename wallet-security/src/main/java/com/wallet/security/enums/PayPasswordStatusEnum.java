package com.wallet.security.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 支付密码状态。
 *
 * <p>取值随 pay_password.password_status 持久化，属于跨端协议的一部分，
 * 修改取值等同协议升级。</p>
 */
@Getter
@AllArgsConstructor
public enum PayPasswordStatusEnum {

    /** 未设置支付密码。 */
    NOT_SET("未设置"),

    /** 支付密码已启用。 */
    ENABLED("已启用"),

    /** 支付密码已停用。 */
    DISABLED("已停用"),

    ;

    /** 状态说明。 */
    private final String description;

}
