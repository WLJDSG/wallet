package com.wallet.security.token;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.wallet.security.enums.IdentityPurposeEnum;
import lombok.Data;

import java.io.Serializable;

/**
 * 支付密码设置、修改、重置或生物注册使用的一次性身份票据。
 *
 * <p>票据用途不可互换，消费时必须同时核对 uid、purpose 与版本快照，防止将短信重置
 * 票据用于修改密码或注册生物凭证。</p>
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PayIdentityToken implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 票据所属用户ID。 */
    private Long uid;

    /**
     * 票据用途。序列化时落库等价于 {@link IdentityPurposeEnum#name()}。
     */
    private IdentityPurposeEnum purpose;

    /** 签发时支付密码版本。 */
    private Integer passwordVersion;

    /** 签发时支付安全版本。 */
    private Integer securityVersion;

    /** 生物注册来源订单号。 */
    private String orderNo;

    /** 生物注册来源订单类型。 */
    private String orderType;

}
