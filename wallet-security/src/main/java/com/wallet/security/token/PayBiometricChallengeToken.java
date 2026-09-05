package com.wallet.security.token;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * KV 存储中保存的生物支付签名挑战快照。
 *
 * <p>客户端私钥签名的规范化载荷由本快照生成，完整绑定 Credential、订单与金额。
 * 挑战只能验签一次，且过期后不能换取支付授权。</p>
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PayBiometricChallengeToken implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 挑战所属用户ID。 */
    private Long uid;

    /** 挑战使用的生物支付凭证ID。 */
    private String credentialId;

    /** 绑定订单号。 */
    private String orderNo;

    /** 绑定订单类型。 */
    private String orderType;

    /** 服务端确认的订单金额。 */
    private BigDecimal amount;

    /** 订单币种。 */
    private String currency;

    /** 服务端安全随机生成的一次性数，用于防止历史签名重放。 */
    private String nonce;

    /** 挑战过期时间戳，单位毫秒。 */
    private Long expiresAt;

}
