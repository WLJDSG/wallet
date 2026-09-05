package com.wallet.security.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 余额支付授权方式。
 *
 * <p>取值写入支付授权票据并落审计，属于跨端协议的一部分，
 * 修改取值等同协议升级。</p>
 */
@Getter
@AllArgsConstructor
public enum AuthorizeMethodEnum {

    /** 支付密码授权。 */
    PASSWORD("支付密码"),

    /** 生物签名授权。 */
    BIOMETRIC("生物签名"),

    /** 未设置支付密码时的二次确认授权（存量用户过渡通道）。 */
    LEGACY_CONFIRM("未设密码二次确认"),

    ;

    /** 授权方式说明。 */
    private final String description;

}
