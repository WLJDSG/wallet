package com.wallet.channel.model;

import java.util.Map;

/**
 * 二段式扣款确认请求（如 PayPal execute payment：用户在渠道侧授权后，商户主动发起扣款确认）。
 *
 * @param channelCode 渠道编码
 * @param orderNo     业务订单号
 * @param outTradeNo  交易号（PayPal 场景即 paymentId）
 * @param extras      渠道授权凭据（如 PayPal payerId），内核不解析
 */
public record ConfirmRequest(String channelCode, String orderNo, String outTradeNo, Map<String, Object> extras) {
}
