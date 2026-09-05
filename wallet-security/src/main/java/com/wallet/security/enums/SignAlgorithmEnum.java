package com.wallet.security.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 生物凭证跨端签名算法。
 *
 * <p>取值随 pay_biometric_credential.algorithm 持久化，属于跨端协议的一部分。
 * 协议 v1 只接受 EC P-256 + SHA-256 的 ECDSA（DER 编码签名），
 * 新增算法等同协议升级。</p>
 */
@Getter
@AllArgsConstructor
public enum SignAlgorithmEnum {

    /** EC P-256 曲线 + SHA-256 摘要的 ECDSA，签名为 ASN.1 DER 编码。 */
    EC_P256_SHA256("ECDSA P-256/SHA-256"),

    ;

    /** 算法说明。 */
    private final String description;

}
