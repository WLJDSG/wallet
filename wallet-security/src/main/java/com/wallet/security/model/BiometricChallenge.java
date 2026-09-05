package com.wallet.security.model;

import lombok.Data;

/**
 * 生物识别支付签名挑战。
 */
@Data
public class BiometricChallenge {

    /** 一次性挑战ID。 */
    private String challengeId;

    /** 服务端生成的规范化载荷，客户端必须按UTF-8字节原样签名。 */
    private String signPayload;

    /** 挑战剩余有效秒数。 */
    private Long expiresIn;
}
