package com.wallet.pay.model;

import java.time.LocalDateTime;

/**
 * 创建支付单结果。
 *
 * @param orderNo    钱包支付单号
 * @param expireTime 过期时间（之后可被超时关单）
 */
public record CreateOrderResult(String orderNo, LocalDateTime expireTime) {
}
