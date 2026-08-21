package com.wallet.channel.model;

/**
 * 渠道查询结果。
 *
 * @param paid            渠道侧是否已支付成功
 * @param thirdOutTradeNo 渠道侧交易号，可为 null
 */
public record QueryResult(boolean paid, String thirdOutTradeNo) {

    public static QueryResult unpaid() {
        return new QueryResult(false, null);
    }
}
