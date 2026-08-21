package com.wallet.channel.support;

import com.wallet.channel.action.CallbackAction;
import com.wallet.channel.action.PayAction;
import com.wallet.channel.model.CallbackRequest;
import com.wallet.channel.model.CallbackResult;
import com.wallet.channel.model.PayRequest;
import com.wallet.channel.model.TradeInfo;

/**
 * 只实现 PAY + CALLBACK 的渠道（模拟 PayPal 式无主动查询能力的渠道）。
 */
public class PayOnlyChannel implements PayAction, CallbackAction {

    private final String code;

    public PayOnlyChannel(String code) {
        this.code = code;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public Object pay(PayRequest request, TradeInfo trade) {
        return "REDIRECT:" + trade.outTradeNo();
    }

    @Override
    public CallbackResult onCallback(CallbackRequest request) {
        return CallbackResult.builder().paid(true).ackBody("OK").build();
    }
}
