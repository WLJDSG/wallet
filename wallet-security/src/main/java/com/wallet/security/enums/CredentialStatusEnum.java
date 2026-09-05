package com.wallet.security.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 生物支付凭证状态。
 *
 * <p>取值随 pay_biometric_credential.status 持久化，属于跨端协议的一部分，
 * 修改取值等同协议升级。凭证可用性由本状态与密码版本、安全版本共同决定，
 * 不能只判断状态字段。</p>
 */
@Getter
@AllArgsConstructor
public enum CredentialStatusEnum {

    /** 凭证启用中，可参与生物签名授权。 */
    ENABLED("已启用"),

    /** 凭证已停用（密码变更、用户解绑等），保留记录用于追溯。 */
    DISABLED("已停用"),

    /** 凭证已撤销，用户全量撤销时落此状态。 */
    REVOKED("已撤销"),

    ;

    /** 状态说明。 */
    private final String description;

}
