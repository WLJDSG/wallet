package com.wallet.security.token;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.wallet.security.enums.BiometricPlatformEnum;
import lombok.Data;

import java.io.Serializable;

/**
 * 生物凭证注册授权和注册会话快照。
 *
 * <p>该快照串联“支付密码身份校验 -&gt; 注册会话 -&gt; 公钥持有证明”三个步骤。
 * 密码或安全版本变化时，未完成的注册会话必须失效。</p>
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PayBiometricRegistrationToken implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 注册用户ID。 */
    private Long uid;

    /** 来源支付订单号，设置页主动开通时为空。 */
    private String orderNo;

    /** 来源订单类型。 */
    private String orderType;

    /**
     * 原生平台。序列化时落库等价于 {@link BiometricPlatformEnum#name()}。
     */
    private BiometricPlatformEnum platform;

    /** 注册授权时支付密码版本。 */
    private Integer passwordVersion;

    /** 注册授权时支付安全版本。 */
    private Integer securityVersion;

    /** 注册公钥持有证明使用的一次性随机数。 */
    private String nonce;

    /** 生物注册授权票据摘要，仅保存摘要以避免 KV 存储中泄露原始票据。 */
    private String enrollmentTokenHash;

}
