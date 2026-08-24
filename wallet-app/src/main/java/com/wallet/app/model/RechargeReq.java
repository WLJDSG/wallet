package com.wallet.app.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;

/**
 * 模拟充值请求。
 *
 * @param amount 充值金额，单位分
 */
@Schema(description = "模拟充值请求")
public record RechargeReq(
    @Schema(description = "充值金额，单位分", example = "10000")
    @Positive(message = "充值金额必须大于 0") long amount) {
}
