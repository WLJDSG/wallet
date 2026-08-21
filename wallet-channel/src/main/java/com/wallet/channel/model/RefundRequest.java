package com.wallet.channel.model;

import java.util.Map;

/**
 * 退款请求（编排层入参）。
 *
 * @param channelCode   渠道编码
 * @param orderNo       业务订单号
 * @param outTradeNo    原支付交易号
 * @param refundOrderNo 调用方生成的退款单号
 * @param amount        申请退款金额，单位分（编排层按可退金额钳制）
 * @param outRefund     是否为外部已退款的补录（线下已退等）：true 时不调用渠道，仅登记退款单并推进状态
 * @param userId        用户标识
 * @param extras        渠道扩展参数，内核不解析
 */
public record RefundRequest(String channelCode, String orderNo, String outTradeNo, String refundOrderNo,
    long amount, boolean outRefund, Long userId, Map<String, Object> extras) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private String channelCode;
        private String orderNo;
        private String outTradeNo;
        private String refundOrderNo;
        private long amount;
        private boolean outRefund;
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

        public Builder outRefund(boolean outRefund) {
            this.outRefund = outRefund;
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

        public RefundRequest build() {
            return new RefundRequest(channelCode, orderNo, outTradeNo, refundOrderNo, amount, outRefund, userId,
                extras);
        }
    }
}
