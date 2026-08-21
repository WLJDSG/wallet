package com.wallet.app.model;

/**
 * 模拟充值请求。
 *
 * @param amount 充值金额，单位分
 */
public record RechargeReq(long amount) {
}
