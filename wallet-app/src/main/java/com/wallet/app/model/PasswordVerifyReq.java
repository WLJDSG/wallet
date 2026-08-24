package com.wallet.app.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * 校验支付密码并签发票据请求。
 *
 * @param password 支付密码
 * @param orderNo  要支付的支付单号
 * @param amount   支付金额，单位分（票据将绑定这二者）
 */
public record PasswordVerifyReq(@NotBlank(message = "密码不能为空") String password,
                                @NotBlank(message = "支付单号不能为空") String orderNo,
                                @Positive(message = "支付金额必须大于 0") long amount) {
}
