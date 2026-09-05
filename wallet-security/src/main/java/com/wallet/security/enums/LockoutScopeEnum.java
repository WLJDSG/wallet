package com.wallet.security.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 支付密码失败计数与锁定的作用域。
 *
 * <p>余额支付与修改支付密码是两条独立的密码校验通道，各自维护失败计数、
 * 锁定 KV 键与锁定时长，一个通道被锁定不影响另一个通道继续使用。
 * keyInfix 参与 KV 键模板拼接，属于存储协议的一部分，不可随意变更。</p>
 */
@Getter
@AllArgsConstructor
public enum LockoutScopeEnum {

    /** 余额支付前的密码校验；键模板沿用既有生产键（无中缀）。 */
    BALANCE_PAY("", "余额支付"),

    /** 修改支付密码（含开通生物凭证）前的当前密码校验。 */
    PASSWORD_CHANGE("change:", "修改支付密码"),

    ;

    /** KV 键中缀：拼接在 "pwd:" 之后，BALANCE_PAY 为空串以保持既有生产键不变。 */
    private final String keyInfix;

    /** 作用域说明。 */
    private final String description;

}
