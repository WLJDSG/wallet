package com.wallet.security.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 生物支付凭证停用或撤销原因。
 *
 * <p>取值随 pay_biometric_credential.disabled_reason 持久化，
 * 属于跨端协议的一部分，修改取值等同协议升级。</p>
 */
@Getter
@AllArgsConstructor
public enum CredentialDisabledReasonEnum {

    /** 支付密码修改或重置，历史凭证全部停用。 */
    PASSWORD_CHANGED("密码变更"),

    /** 用户主动解绑单个凭证。 */
    USER_UNBOUND("用户解绑"),

    /** 用户全量撤销全部凭证。 */
    USER_REVOKE_ALL("用户全量撤销"),

    ;

    /** 原因说明。 */
    private final String description;

}
