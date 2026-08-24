package com.wallet.app.model;

import jakarta.validation.constraints.Positive;

/**
 * 领券请求。
 *
 * @param couponId 券模板 ID
 */
public record CouponTakeReq(@Positive(message = "券模板 ID 不正确") long couponId) {
}
