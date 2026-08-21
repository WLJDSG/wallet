package com.wallet.channel.support;

import com.wallet.channel.action.CallbackAction;
import com.wallet.channel.action.CancelAction;
import com.wallet.channel.action.ConfirmAction;
import com.wallet.channel.action.PayAction;
import com.wallet.channel.action.QueryAction;
import com.wallet.channel.action.RefundAction;
import com.wallet.channel.model.CallbackRequest;
import com.wallet.channel.model.CallbackResult;
import com.wallet.channel.model.CancelRequest;
import com.wallet.channel.model.ChannelRefundRequest;
import com.wallet.channel.model.ConfirmRequest;
import com.wallet.channel.model.ConfirmResult;
import com.wallet.channel.model.PayRequest;
import com.wallet.channel.model.QueryRequest;
import com.wallet.channel.model.QueryResult;
import com.wallet.channel.model.RefundResult;
import com.wallet.channel.model.TradeInfo;

/**
 * 全能力假渠道，各动作行为可配置、调用次数可断言。
 */
public class FakeChannel implements PayAction, QueryAction, RefundAction, CancelAction, CallbackAction,
    ConfirmAction {

    private final String code;

    public boolean queryPaid = false;
    public boolean refundSuccess = true;
    public boolean cancelResult = true;
    public boolean callbackPaid = true;
    public boolean callbackReQuery = false;
    public boolean confirmSuccess = true;
    public RuntimeException refundException = null;

    public int payCount;
    public int queryCount;
    public int refundCount;
    public int cancelCount;
    public int callbackCount;
    public int confirmCount;

    public FakeChannel(String code) {
        this.code = code;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public Object pay(PayRequest request, TradeInfo trade) {
        payCount++;
        return "PAY_URL:" + trade.outTradeNo();
    }

    @Override
    public QueryResult query(QueryRequest request) {
        queryCount++;
        return new QueryResult(queryPaid, queryPaid ? "THIRD-1" : null);
    }

    @Override
    public RefundResult refund(ChannelRefundRequest request) {
        refundCount++;
        if (refundException != null) {
            throw refundException;
        }
        if (refundSuccess) {
            return RefundResult.ok(null);
        }
        return RefundResult.fail("credit check rejected");
    }

    @Override
    public boolean cancel(CancelRequest request) {
        cancelCount++;
        return cancelResult;
    }

    @Override
    public CallbackResult onCallback(CallbackRequest request) {
        callbackCount++;
        return CallbackResult.builder().paid(callbackPaid).thirdOutTradeNo("THIRD-CB")
            .reQueryRequired(callbackReQuery).ackBody("OK").build();
    }

    @Override
    public ConfirmResult confirm(ConfirmRequest request) {
        confirmCount++;
        if (confirmSuccess) {
            return ConfirmResult.ok("THIRD-EX");
        }
        return ConfirmResult.fail("declined");
    }
}
