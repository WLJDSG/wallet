package com.wallet.contract.channel.model;

/**
 * 关闭/取消交易请求。
 *
 * @param channelCode     渠道编码
 * @param orderNo         业务订单号
 * @param outTradeNo      交易号
 * @param thirdOutTradeNo 渠道侧交易号，可为 null
 */
public record CancelRequest(String channelCode, String orderNo, String outTradeNo, String thirdOutTradeNo) {
}
