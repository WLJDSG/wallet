package com.wallet.app.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * 校验支付密码并签发票据请求。
 *
 * @param password 支付密码
 * @param orderNo  要支付的支付单号
 * @param amount   支付金额，单位分（票据将绑定这二者）
 */
@Schema(description = "校验支付密码并签发票据请求（票据绑定订单与金额）")
public record PasswordVerifyReq(
    @Schema(description = "支付密码", example = "123456")
    @NotBlank(message = "密码不能为空") String password,
    @Schema(description = "要支付的支付单号")
    @NotBlank(message = "支付单号不能为空") String orderNo,
    @Schema(description = "支付金额，单位分", example = "5000")
    @Positive(message = "支付金额必须大于 0") long amount) {
}
