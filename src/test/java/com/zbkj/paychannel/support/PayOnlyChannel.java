package com.zbkj.paychannel.support;

import com.zbkj.paychannel.model.CallbackCommand;
import com.zbkj.paychannel.model.CallbackResult;
import com.zbkj.paychannel.model.PayCommand;
import com.zbkj.paychannel.model.PayOrderSnapshot;
import com.zbkj.paychannel.provider.CallbackProvider;
import com.zbkj.paychannel.provider.PayProvider;

/**
 * 只实现 PAY + CALLBACK 的渠道（模拟 PayPal 式无主动查询能力的渠道）。
 */
public class PayOnlyChannel implements PayProvider, CallbackProvider {

    private final String code;

    public PayOnlyChannel(String code) {
        this.code = code;
    }

    @Override
    public String channelCode() {
        return code;
    }

    @Override
    public Object pay(PayCommand command, PayOrderSnapshot payOrder) {
        return "REDIRECT:" + payOrder.getOutTradeNo();
    }

    @Override
    public CallbackResult handleCallback(CallbackCommand command) {
        return CallbackResult.builder().paid(true).ackBody("OK").build();
    }
}
