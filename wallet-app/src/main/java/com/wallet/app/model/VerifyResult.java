package com.wallet.app.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 校验通过后签发的一次性授权票据。
 *
 * @param ticket 票据（提交支付时带上，TTL 300 秒，一次性）
 */
@Schema(description = "密码校验结果")
public record VerifyResult(
    @Schema(description = "一次性授权票据（TTL 300 秒，提交支付时消费）") String ticket) {
}
