package com.wallet.app.model;

import jakarta.validation.constraints.Positive;

/**
 * 模拟充值请求。
 *
 * @param amount 充值金额，单位分
 */
public record RechargeReq(@Positive(message = "充值金额必须大于 0") long amount) {
}
