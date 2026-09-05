package com.wallet.security.token;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.wallet.security.enums.AuthorizeMethodEnum;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * KV 存储中保存的一次性余额支付授权票据快照。
 *
 * <p>票据同时绑定用户、订单、金额、币种和安全版本。余额扣减前必须重新读取订单
 * 并原子消费票据，不得仅依赖本快照判定支付可用性。</p>
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PayAuthorizationToken implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 授权用户ID。 */
    private Long uid;

    /** 绑定订单号。 */
    private String orderNo;

    /** 绑定订单类型。 */
    private String orderType;

    /** 授权时服务端确认的实付金额。 */
    private BigDecimal amount;

    /** 授权币种。 */
    private String currency;

    /**
     * 授权方式。序列化时落库等价于 {@link AuthorizeMethodEnum#name()}。
     */
    private AuthorizeMethodEnum authorizationMethod;

    /** 生物授权使用的Credential ID，其他授权方式为空。 */
    private String credentialId;

    /** 授权时支付密码版本，密码变更后用于使历史票据失效。 */
    private Integer passwordVersion;

    /** 授权时全局支付安全版本，全量撤销凭证等操作后用于统一失效。 */
    private Integer securityVersion;

    /** 票据协议版本。 */
    private String policyVersion;

    /** 签发时间戳，单位毫秒。 */
    private Long issuedAt;

    /** 过期时间戳，单位毫秒。 */
    private Long expiresAt;

}
