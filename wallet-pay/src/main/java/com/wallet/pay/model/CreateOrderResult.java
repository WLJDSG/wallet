package com.wallet.pay.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 创建支付单结果。
 *
 * @param orderNo    钱包支付单号
 * @param expireTime 过期时间（之后可被超时关单）
 */
@Schema(description = "创建支付单结果")
public record CreateOrderResult(
    @Schema(description = "钱包支付单号") String orderNo,
    @Schema(description = "过期时间（之后可被超时关单）") LocalDateTime expireTime) {
}
