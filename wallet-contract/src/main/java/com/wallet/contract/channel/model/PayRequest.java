package com.wallet.contract.channel.model;

import java.util.Map;

/**
 * 发起支付请求。
 *
 * <p>金额语义：{@code amount} 为本次应付金额（单位分，渠道手续费由 {@code FeeRule} 在编排层统一加成）。</p>
 *
 * @param channelCode    渠道编码（如 "MOCK"、"ANTOM"），需与渠道实现的 code() 一致
 * @param orderNo        业务订单号（钱包工程里即支付主单号）
 * @param orderType      业务订单类型（透传给渠道拼订单描述等），可为 null
 * @param amount         应付金额，单位分（未含渠道手续费）
 * @param currency       币种（ISO 4217，如 TWD/JPY/CNY）
 * @param lastOutTradeNo 同一订单上一次发起支付的交易号；非空时编排器会先查证并关闭上一笔未支付交易
 * @param userId         用户标识，内核不解析
 * @param clientIp       客户端 IP（部分渠道风控必填），可为 null
 * @param extras         渠道扩展参数，内核不解析，原样透传给渠道实现
 */
public record PayRequest(String channelCode, String orderNo, String orderType, long amount, String currency,
    String lastOutTradeNo, Long userId, String clientIp, Map<String, Object> extras) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private String channelCode;
        private String orderNo;
        private String orderType;
        private long amount;
        private String currency;
        private String lastOutTradeNo;
        private Long userId;
        private String clientIp;
        private Map<String, Object> extras;

        public Builder channelCode(String channelCode) {
            this.channelCode = channelCode;
            return this;
        }

        public Builder orderNo(String orderNo) {
            this.orderNo = orderNo;
            return this;
        }

        public Builder orderType(String orderType) {
            this.orderType = orderType;
            return this;
        }

        public Builder amount(long amount) {
            this.amount = amount;
            return this;
        }

        public Builder currency(String currency) {
            this.currency = currency;
            return this;
        }

        public Builder lastOutTradeNo(String lastOutTradeNo) {
            this.lastOutTradeNo = lastOutTradeNo;
            return this;
        }

        public Builder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public Builder clientIp(String clientIp) {
            this.clientIp = clientIp;
            return this;
        }

        public Builder extras(Map<String, Object> extras) {
            this.extras = extras;
            return this;
        }

        public PayRequest build() {
            return new PayRequest(channelCode, orderNo, orderType, amount, currency, lastOutTradeNo, userId,
                clientIp, extras);
        }
    }
}
