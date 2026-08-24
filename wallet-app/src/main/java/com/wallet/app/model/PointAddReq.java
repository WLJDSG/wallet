package com.wallet.app.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;

/**
 * 模拟发积分请求。
 *
 * @param count 积分数
 */
@Schema(description = "模拟发积分请求")
public record PointAddReq(
    @Schema(description = "积分数", example = "1000")
    @Positive(message = "积分数必须大于 0") long count) {
}
