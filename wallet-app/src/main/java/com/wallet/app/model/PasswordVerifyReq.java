package com.wallet.app.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 校验支付密码并签发票据请求。
 *
 * @param password 支付密码
 * @param orderNo  要支付的支付单号
 * @param amount   兼容字段，服务端忽略并从支付单实时计算余额段金额
 */
@Schema(description = "校验支付密码并签发票据请求（金额由服务端支付单事实决定）")
public record PasswordVerifyReq(
    @Schema(description = "支付密码", example = "123456")
    @NotBlank(message = "密码不能为空") String password,
    @Schema(description = "要支付的支付单号")
    @NotBlank(message = "支付单号不能为空") String orderNo,
    @Schema(description = "兼容字段，服务端忽略", example = "5000", deprecated = true)
    Long amount) {
}
