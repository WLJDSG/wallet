package com.wallet.security.model;

import lombok.Data;

/**
 * 支付授权结果。
 */
@Data
public class AuthorizationResult {

    /** 与用户、订单、金额和币种绑定的一次性余额支付授权票据。 */
    private String payAuthorizationToken;

    /** 授权票据剩余有效秒数。 */
    private Long expiresIn;

    /** 生物凭证注册票据，仅符合注册条件时返回。 */
    private String biometricEnrollmentToken;
}
