package com.zbkj.paychannel.support;

import com.zbkj.paychannel.model.CallbackCommand;
import com.zbkj.paychannel.model.CallbackResult;
import com.zbkj.paychannel.model.CancelCommand;
import com.zbkj.paychannel.model.ChannelRefundCommand;
import com.zbkj.paychannel.model.ExecutePaymentCommand;
import com.zbkj.paychannel.model.ExecutePaymentResult;
import com.zbkj.paychannel.model.PayCommand;
import com.zbkj.paychannel.model.PayOrderSnapshot;
import com.zbkj.paychannel.model.QueryCommand;
import com.zbkj.paychannel.model.QueryResult;
import com.zbkj.paychannel.provider.CallbackProvider;
import com.zbkj.paychannel.provider.CancelProvider;
import com.zbkj.paychannel.provider.ExecutePaymentProvider;
import com.zbkj.paychannel.provider.PayProvider;
import com.zbkj.paychannel.provider.QueryProvider;
import com.zbkj.paychannel.provider.RefundProvider;

/**
 * 全能力模拟渠道，各动作行为可配置、调用次数可断言。
 */
public class MockChannel implements PayProvider, QueryProvider, RefundProvider, CancelProvider, CallbackProvider,
    ExecutePaymentProvider {

    private final String code;

    public boolean queryPaid = false;
    public boolean refundSuccess = true;
    public boolean cancelResult = true;
    public boolean callbackPaid = true;
    public boolean callbackReQuery = false;
    public boolean executeSuccess = true;
    public RuntimeException refundException = null;

    public int payCount;
    public int queryCount;
    public int refundCount;
    public int cancelCount;
    public int callbackCount;
    public int executeCount;

    public MockChannel(String code) {
        this.code = code;
    }

    @Override
    public String channelCode() {
        return code;
    }

    @Override
    public Object pay(PayCommand command, PayOrderSnapshot payOrder) {
        payCount++;
        return "PAY_URL:" + payOrder.getOutTradeNo();
    }

    @Override
    public QueryResult query(QueryCommand command) {
        queryCount++;
        return QueryResult.builder().paid(queryPaid).thirdOutTradeNo(queryPaid ? "THIRD-1" : null).build();
    }

    @Override
    public com.zbkj.paychannel.model.RefundResult refund(ChannelRefundCommand command) {
        refundCount++;
        if (refundException != null) {
            throw refundException;
        }
        return com.zbkj.paychannel.model.RefundResult.builder().success(refundSuccess)
            .failReason(refundSuccess ? null : "credit check rejected").build();
    }

    @Override
    public boolean cancel(CancelCommand command) {
        cancelCount++;
        return cancelResult;
    }

    @Override
    public CallbackResult handleCallback(CallbackCommand command) {
        callbackCount++;
        return CallbackResult.builder().paid(callbackPaid).thirdOutTradeNo("THIRD-CB")
            .reQueryRequired(callbackReQuery).ackBody("OK").build();
    }

    @Override
    public ExecutePaymentResult executePayment(ExecutePaymentCommand command) {
        executeCount++;
        return ExecutePaymentResult.builder().success(executeSuccess).thirdOutTradeNo("THIRD-EX").build();
    }
}
