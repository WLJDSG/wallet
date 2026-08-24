package com.wallet.app.model;

import jakarta.validation.constraints.Positive;

/**
 * 模拟发积分请求。
 *
 * @param count 积分数
 */
public record PointAddReq(@Positive(message = "积分数必须大于 0") long count) {
}
