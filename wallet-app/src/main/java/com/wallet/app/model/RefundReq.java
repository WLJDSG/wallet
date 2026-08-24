package com.wallet.app.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * 发起退款请求。
 *
 * @param orderNo 支付单号
 * @param amount  退款金额，单位分（按 CHANNEL→MONEY→POINT 分摊）
 * @param reason  退款原因
 */
public record RefundReq(@NotBlank(message = "支付单号不能为空") String orderNo,
                        @Positive(message = "退款金额必须大于 0") long amount,
                        String reason) {
}
