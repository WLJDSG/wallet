package com.wallet.contract.channel.model;

/**
 * 渠道回调解析结果（渠道实现产出，验签失败应抛 CALLBACK_VERIFY_FAILED 而不是返回本对象）。
 *
 * @param paid            回调声明的支付结果
 * @param thirdOutTradeNo 渠道侧交易号
 * @param reQueryRequired 是否需要以主动查询结果为准（不信任回调报文的渠道设 true；
 *                        该渠道必须同时实现 QueryAction）
 * @param ackBody         应答渠道的报文体（无论处理是否幂等跳过都会原样返回给渠道）
 */
public record CallbackResult(boolean paid, String thirdOutTradeNo, boolean reQueryRequired, String ackBody) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private boolean paid;
        private String thirdOutTradeNo;
        private boolean reQueryRequired;
        private String ackBody;

        public Builder paid(boolean paid) {
            this.paid = paid;
            return this;
        }

        public Builder thirdOutTradeNo(String thirdOutTradeNo) {
            this.thirdOutTradeNo = thirdOutTradeNo;
            return this;
        }

        public Builder reQueryRequired(boolean reQueryRequired) {
            this.reQueryRequired = reQueryRequired;
            return this;
        }

        public Builder ackBody(String ackBody) {
            this.ackBody = ackBody;
            return this;
        }

        public CallbackResult build() {
            return new CallbackResult(paid, thirdOutTradeNo, reQueryRequired, ackBody);
        }
    }
}
