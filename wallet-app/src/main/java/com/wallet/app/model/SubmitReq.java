package com.wallet.app.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 提交支付请求。
 *
 * @param orderNo 支付单号
 * @param ticket  支付密码授权票据（含余额/积分/券分段时必填，条件校验在服务层）
 */
@Schema(description = "提交支付请求")
public record SubmitReq(
    @Schema(description = "支付单号")
    @NotBlank(message = "支付单号不能为空") String orderNo,
    @Schema(description = "支付密码授权票据（含资产段时必填）") String ticket) {
}
