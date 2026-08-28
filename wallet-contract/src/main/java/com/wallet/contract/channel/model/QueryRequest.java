package com.wallet.contract.channel.model;

/**
 * 主动查询支付结果请求。
 *
 * @param channelCode    渠道编码
 * @param orderNo        业务订单号
 * @param outTradeNo     交易号
 * @param thirdOutTradeNo 渠道侧交易号（部分渠道查询必填），可为 null
 */
public record QueryRequest(String channelCode, String orderNo, String outTradeNo, String thirdOutTradeNo) {

    public QueryRequest(String channelCode, String orderNo, String outTradeNo) {
        this(channelCode, orderNo, outTradeNo, null);
    }
}
