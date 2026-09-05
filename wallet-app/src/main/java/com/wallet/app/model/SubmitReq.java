package com.wallet.app.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 提交支付请求。
 *
 * @param orderNo 支付单号
 * @param ticket  支付安全授权票据（含余额段时必填，密码或生物识别均可签发）
 */
@Schema(description = "提交支付请求")
public record SubmitReq(
    @Schema(description = "支付单号")
    @NotBlank(message = "支付单号不能为空") String orderNo,
    @Schema(description = "一次性支付授权票据（含余额段时必填）") String ticket) {
}
