package com.wallet.security.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 硬件密钥证明与 App 完整性证明结果。
 *
 * <p>取值随 pay_biometric_credential 的 key_attestation_status、
 * app_integrity_status 两个字段持久化，属于跨端协议的一部分。
 * 当前协议 v1 注册时不执行证明校验，统一记录为 {@link #UNVERIFIED}，
 * VERIFIED/FAILED 预留给后续接入 Key Attestation 与 App 完整性校验。</p>
 */
@Getter
@AllArgsConstructor
public enum AttestationStatusEnum {

    /** 证明校验通过。 */
    VERIFIED("已验证"),

    /** 未执行证明校验（当前协议 v1 的默认残余风险记录）。 */
    UNVERIFIED("未验证"),

    /** 证明校验失败。 */
    FAILED("验证失败"),

    ;

    /** 结果说明。 */
    private final String description;

}
