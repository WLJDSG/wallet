package com.wallet.channel.serviceImpl.support;

/**
 * mock 渠道的下单返回（前端拉起支付参数）。
 *
 * @param partNo 交易号
 * @param payUrl 模拟支付 URL
 */
public record MockPayload(String partNo, String payUrl) {
}
