package com.wallet.app.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * 发起退款请求。
 *
 * @param orderNo 支付单号
 * @param amount  退款金额，单位分（按 CHANNEL→MONEY→POINT 分摊）
 * @param reason  退款原因
 */
@Schema(description = "发起退款请求")
public record RefundReq(
    @Schema(description = "支付单号")
    @NotBlank(message = "支付单号不能为空") String orderNo,
    @Schema(description = "退款金额，单位分（按 CHANNEL→MONEY→POINT 逆序分摊）", example = "1000")
    @Positive(message = "退款金额必须大于 0") long amount,
    @Schema(description = "退款原因") String reason) {
}
