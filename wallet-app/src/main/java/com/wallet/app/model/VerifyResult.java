package com.wallet.app.model;

/**
 * 校验通过后签发的一次性授权票据。
 *
 * @param ticket 票据（提交支付时带上，TTL 300 秒，一次性）
 */
public record VerifyResult(String ticket) {
}
