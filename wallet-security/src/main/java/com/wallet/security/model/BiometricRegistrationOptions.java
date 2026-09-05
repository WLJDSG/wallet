package com.wallet.security.model;

import lombok.Data;

/**
 * 生物支付注册会话。
 */
@Data
public class BiometricRegistrationOptions {

    /** 一次性注册会话ID。 */
    private String registrationId;

    /** 客户端私钥必须签名的注册随机数。 */
    private String nonce;

    /** 注册会话剩余有效秒数。 */
    private Long expiresIn;
}
