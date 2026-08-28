package com.wallet.contract.channel.model;

import com.wallet.common.enums.PayState;

/**
 * 交易单快照（调用方持久化模型在内核侧的只读视图；钱包工程里即三方支付分段）。
 *
 * @param orderNo          业务订单号
 * @param outTradeNo       交易号
 * @param channelCode      渠道编码
 * @param state            当前状态
 * @param amount           实际请求渠道的支付金额，单位分（含手续费）
 * @param currency         币种
 * @param refundableAmount 剩余可退金额，单位分
 * @param thirdOutTradeNo  渠道侧交易号，可为 null
 */
public record TradeInfo(String orderNo, String outTradeNo, String channelCode, PayState state, long amount,
    String currency, long refundableAmount, String thirdOutTradeNo) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private String orderNo;
        private String outTradeNo;
        private String channelCode;
        private PayState state;
        private long amount;
        private String currency;
        private long refundableAmount;
        private String thirdOutTradeNo;

        public Builder orderNo(String orderNo) {
            this.orderNo = orderNo;
            return this;
        }

        public Builder outTradeNo(String outTradeNo) {
            this.outTradeNo = outTradeNo;
            return this;
        }

        public Builder channelCode(String channelCode) {
            this.channelCode = channelCode;
            return this;
        }

        public Builder state(PayState state) {
            this.state = state;
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

        public Builder refundableAmount(long refundableAmount) {
            this.refundableAmount = refundableAmount;
            return this;
        }

        public Builder thirdOutTradeNo(String thirdOutTradeNo) {
            this.thirdOutTradeNo = thirdOutTradeNo;
            return this;
        }

        public TradeInfo build() {
            return new TradeInfo(orderNo, outTradeNo, channelCode, state, amount, currency, refundableAmount,
                thirdOutTradeNo);
        }
    }
}
