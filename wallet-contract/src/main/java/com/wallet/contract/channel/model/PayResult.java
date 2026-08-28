package com.wallet.contract.channel.model;

/**
 * 发起支付结果。
 *
 * @param orderNo        业务订单号
 * @param outTradeNo     本次交易号（由 TradeStore 创建交易单时生成）
 * @param amount         实际请求渠道的金额，单位分（已含手续费）
 * @param currency       币种
 * @param channelPayload 渠道返回的前端拉起支付所需参数（jsConfig/二维码/跳转URL等），
 *                       具体类型由各渠道实现约定
 * @param queryable      该渠道是否支持主动查询：只有 true 的交易才应进轮询兜底队列
 */
public record PayResult(String orderNo, String outTradeNo, long amount, String currency, Object channelPayload,
    boolean queryable) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private String orderNo;
        private String outTradeNo;
        private long amount;
        private String currency;
        private Object channelPayload;
        private boolean queryable;

        public Builder orderNo(String orderNo) {
            this.orderNo = orderNo;
            return this;
        }

        public Builder outTradeNo(String outTradeNo) {
            this.outTradeNo = outTradeNo;
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

        public Builder channelPayload(Object channelPayload) {
            this.channelPayload = channelPayload;
            return this;
        }

        public Builder queryable(boolean queryable) {
            this.queryable = queryable;
            return this;
        }

        public PayResult build() {
            return new PayResult(orderNo, outTradeNo, amount, currency, channelPayload, queryable);
        }
    }
}
