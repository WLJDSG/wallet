package com.wallet.app.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;

/**
 * 领券请求。
 *
 * @param couponId 券模板 ID
 */
@Schema(description = "领券请求")
public record CouponTakeReq(
    @Schema(description = "券模板 ID", example = "1")
    @Positive(message = "券模板 ID 不正确") long couponId) {
}
