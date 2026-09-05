package com.wallet.security.model;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 余额扣减前校验并消费支付授权票据的命令。
 *
 * <p>{@code required} 由宿主按业务口径预判（如仅余额支付且实付金额大于 0），
 * 支付安全内核 在 required 为 false 且未携带票据时直接放行；user 可为 null（视同校验失败）。</p>
 */
@Data
public class AuthorizationConsumeCommand {

    /** 服务端策略是否强制本次支付必须授权。 */
    private boolean required;

    /** 客户端携带的一次性支付授权票据，可为空。 */
    private String payAuthorizationToken;

    /** 支付用户身份，null 或非激活状态都会使票据校验失败。 */
    private UserIdentity user;

    /** 支付订单号。 */
    private String orderNo;

    /** 支付订单类型。 */
    private String orderType;

    /** 本次实付金额。 */
    private BigDecimal payPrice;

    /** 本次支付币种。 */
    private String currency;

    /** 客户端环境快照，用于审计。 */
    private ClientInfo clientInfo;
}
