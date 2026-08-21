package com.wallet.channel.model;

import java.util.Map;

/**
 * 下发给渠道实现的退款请求（编排层已完成金额钳制与全额判定）。
 *
 * @param channelCode     渠道编码
 * @param orderNo         业务订单号
 * @param outTradeNo      原支付交易号
 * @param refundOrderNo   退款单号
 * @param amount          最终退款金额，单位分（已钳制到可退金额）
 * @param currency        币种
 * @param thirdOutTradeNo 渠道侧交易号
 * @param allRefund       是否全额退款
 * @param userId          用户标识
 * @param extras          渠道扩展参数
 */
public record ChannelRefundRequest(String channelCode, String orderNo, String outTradeNo, String refundOrderNo,
    long amount, String currency, String thirdOutTradeNo, boolean allRefund, Long userId,
    Map<String, Object> extras) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private String channelCode;
        private String orderNo;
        private String outTradeNo;
        private String refundOrderNo;
        private long amount;
        private String currency;
        private String thirdOutTradeNo;
        private boolean allRefund;
        private Long userId;
        private Map<String, Object> extras;

        public Builder channelCode(String channelCode) {
            this.channelCode = channelCode;
            return this;
        }

        public Builder orderNo(String orderNo) {
            this.orderNo = orderNo;
            return this;
        }

        public Builder outTradeNo(String outTradeNo) {
            this.outTradeNo = outTradeNo;
            return this;
        }

        public Builder refundOrderNo(String refundOrderNo) {
            this.refundOrderNo = refundOrderNo;
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

        public Builder thirdOutTradeNo(String thirdOutTradeNo) {
            this.thirdOutTradeNo = thirdOutTradeNo;
            return this;
        }

        public Builder allRefund(boolean allRefund) {
            this.allRefund = allRefund;
            return this;
        }

        public Builder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public Builder extras(Map<String, Object> extras) {
            this.extras = extras;
            return this;
        }

        public ChannelRefundRequest build() {
            return new ChannelRefundRequest(channelCode, orderNo, outTradeNo, refundOrderNo, amount, currency,
                thirdOutTradeNo, allRefund, userId, extras);
        }
    }
}
