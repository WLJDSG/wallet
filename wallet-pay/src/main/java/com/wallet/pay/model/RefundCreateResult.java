package com.wallet.pay.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 创建退款结果。
 *
 * @param refundNo 退款单号
 * @param state    退款单状态
 */
@Schema(description = "发起退款结果")
public record RefundCreateResult(
    @Schema(description = "退款单号") String refundNo,
    @Schema(description = "退款单状态：SUCCESS / FAIL") String state) {
}
