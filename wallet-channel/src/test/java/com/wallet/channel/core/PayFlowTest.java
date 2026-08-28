package com.wallet.channel.core;

import com.wallet.channel.serviceImpl.support.ChannelServiceImpl;
import com.wallet.common.enums.ActionType;
import com.wallet.common.error.ErrorCode;
import com.wallet.common.enums.PayState;
import com.wallet.common.enums.RefundState;
import com.wallet.contract.channel.error.ChannelException;
import com.wallet.contract.channel.model.CallbackRequest;
import com.wallet.contract.channel.model.ConfirmRequest;
import com.wallet.contract.channel.model.PayRequest;
import com.wallet.contract.channel.model.PayResult;
import com.wallet.contract.channel.model.QueryRequest;
import com.wallet.contract.channel.model.RefundRequest;
import com.wallet.contract.channel.model.TradeInfo;
import com.wallet.channel.support.FakeChannel;
import com.wallet.channel.support.MemoryRefundStore;
import com.wallet.channel.support.MemoryTradeStore;
import com.wallet.channel.support.PayOnlyChannel;
import com.wallet.channel.support.TestHelpers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PayFlowTest {

    private static final String CHANNEL = "MOCK";
    private static final String PAY_ONLY = "PAY_ONLY";

    private FakeChannel channel;
    private MemoryTradeStore trades;
    private MemoryRefundStore refunds;
    private TestHelpers.RecordingListener events;
    private PayFlow flow;
    private ChannelServiceImpl kit;

    @BeforeEach
    void setUp() {
        channel = new FakeChannel(CHANNEL);
        trades = new MemoryTradeStore();
        refunds = new MemoryRefundStore();
        events = new TestHelpers.RecordingListener();
        kit = ChannelServiceImpl.builder()
            .addChannel(channel)
            .addChannel(new PayOnlyChannel(PAY_ONLY))
            .tradeStore(trades)
            .refundStore(refunds)
            .listener(events)
            .build();
        flow = kit.flow();
    }

    private PayRequest.Builder payRequest(String orderNo) {
        return PayRequest.builder().channelCode(CHANNEL).orderNo(orderNo).amount(10000).currency("TWD");
    }

    private PayResult payAndSucceed(String orderNo) {
        PayResult result = flow.pay(payRequest(orderNo).build());
        String ack = flow.callback(callback(orderNo, result.outTradeNo()));
        assertEquals("OK", ack);
        return result;
    }

    private CallbackRequest callback(String orderNo, String outTradeNo) {
        return CallbackRequest.builder().channelCode(CHANNEL).orderNo(orderNo).outTradeNo(outTradeNo)
            .body("{\"result\":\"S\"}").build();
    }

    @Test
    void payCreatesTradeAndAdvancesToPaying() {
        PayResult result = flow.pay(payRequest("O1").build());
        assertNotNull(result.outTradeNo());
        assertEquals(PayState.PAYING, trades.stateOf(result.outTradeNo()));
        assertTrue(result.channelPayload().toString().startsWith("PAY_URL:"));
        assertTrue(result.queryable(), "MOCK 渠道实现了 QueryAction，应参与轮询");
        assertEquals(1, channel.payCount);
    }

    @Test
    void payWithLastTradeClosesPreviousTrade() {
        PayResult first = flow.pay(payRequest("O2").build());
        PayResult second = flow.pay(payRequest("O2").lastOutTradeNo(first.outTradeNo()).build());
        assertEquals(PayState.CLOSED, trades.stateOf(first.outTradeNo()));
        assertEquals(PayState.PAYING, trades.stateOf(second.outTradeNo()));
        assertEquals(1, channel.cancelCount);
    }

    @Test
    void payOnPaidLastTradeThrows() {
        PayResult first = flow.pay(payRequest("O3").build());
        channel.queryPaid = true;
        ChannelException e = assertThrows(ChannelException.class,
            () -> flow.pay(payRequest("O3").lastOutTradeNo(first.outTradeNo()).build()));
        assertEquals(ErrorCode.ORDER_HAS_PAID, e.error());
    }

    /** 回调幂等：重复回调只推进一次状态、只发布一次事件，且每次都返回渠道应答 */
    @Test
    void callbackIsIdempotent() {
        PayResult result = flow.pay(payRequest("O4").build());
        String ack1 = flow.callback(callback("O4", result.outTradeNo()));
        String ack2 = flow.callback(callback("O4", result.outTradeNo()));
        assertEquals("OK", ack1);
        assertEquals("OK", ack2);
        assertEquals(PayState.SUCCESS, trades.stateOf(result.outTradeNo()));
        assertEquals(1, events.paySuccess.size(), "支付成功事件应只发布一次");
    }

    @Test
    void callbackWithReQueryTrustsQueryResult() {
        channel.callbackReQuery = true;
        channel.queryPaid = true;
        PayResult result = flow.pay(payRequest("O5").build());
        flow.callback(callback("O5", result.outTradeNo()));
        assertEquals(PayState.SUCCESS, trades.stateOf(result.outTradeNo()));
        assertEquals(1, channel.queryCount);
    }

    @Test
    void callbackWithReQueryUnpaidThrows() {
        channel.callbackReQuery = true;
        channel.queryPaid = false;
        PayResult result = flow.pay(payRequest("O6").build());
        ChannelException e = assertThrows(ChannelException.class,
            () -> flow.callback(callback("O6", result.outTradeNo())));
        assertEquals(ErrorCode.CALLBACK_QUERY_UNPAID, e.error());
        assertEquals(PayState.PAYING, trades.stateOf(result.outTradeNo()));
        assertEquals(0, events.paySuccess.size());
    }

    @Test
    void queryAdvancesToSuccessOncePaid() {
        PayResult result = flow.pay(payRequest("O7").build());
        QueryRequest query = new QueryRequest(CHANNEL, "O7", result.outTradeNo());
        assertFalse(flow.query(query));
        channel.queryPaid = true;
        assertTrue(flow.query(query));
        assertEquals(PayState.SUCCESS, trades.stateOf(result.outTradeNo()));
        // 已终态后再查询：直接返回 true，不再调渠道
        int queriesSoFar = channel.queryCount;
        assertTrue(flow.query(query));
        assertEquals(queriesSoFar, channel.queryCount);
        assertEquals(1, events.paySuccess.size());
    }

    /** 回归用例：未实现 QUERY 的渠道，轮询调用得到明确异常而不是 NPE 死循环 */
    @Test
    void queryOnPayOnlyChannelThrowsTypedException() {
        PayResult result = flow.pay(PayRequest.builder().channelCode(PAY_ONLY).orderNo("O8")
            .amount(1000).currency("TWD").build());
        assertFalse(result.queryable(), "无 QueryAction 的渠道不应参与轮询");
        ChannelException e = assertThrows(ChannelException.class,
            () -> flow.query(new QueryRequest(PAY_ONLY, "O8", result.outTradeNo())));
        assertEquals(ErrorCode.PAYMENT_ACTION_UNSUPPORTED, e.error());
    }

    @Test
    void refundSuccessReducesRefundableAndPublishesEvent() {
        PayResult paid = payAndSucceed("O9");
        boolean ok = flow.refund(RefundRequest.builder().channelCode(CHANNEL).orderNo("O9")
            .outTradeNo(paid.outTradeNo()).refundOrderNo("R9").amount(4000).build());
        assertTrue(ok);
        assertEquals(RefundState.SUCCESS, refunds.stateOf("R9"));
        assertEquals(6000L, trades.refundableOf(paid.outTradeNo()));
        assertEquals(1, events.refundSuccess.size());
    }

    /** 回归用例：渠道拒绝退款时，退款单落 FAIL 留痕，而不是抛"非法状态变更"后无痕回滚 */
    @Test
    void refundRejectionMarksRefundOrderAsFail() {
        PayResult paid = payAndSucceed("O10");
        channel.refundSuccess = false;
        boolean ok = flow.refund(RefundRequest.builder().channelCode(CHANNEL).orderNo("O10")
            .outTradeNo(paid.outTradeNo()).refundOrderNo("R10").amount(4000).build());
        assertFalse(ok);
        assertEquals(RefundState.FAIL, refunds.stateOf("R10"));
        assertEquals(10000L, trades.refundableOf(paid.outTradeNo()), "可退金额不应被扣减");
        assertEquals(0, events.refundSuccess.size());
    }

    /** 渠道退款抛异常同样留下 FAIL 痕迹再上抛 */
    @Test
    void refundChannelExceptionStillMarksFail() {
        PayResult paid = payAndSucceed("O11");
        channel.refundException = new IllegalStateException("gateway timeout");
        ChannelException e = assertThrows(ChannelException.class,
            () -> flow.refund(RefundRequest.builder().channelCode(CHANNEL).orderNo("O11")
                .outTradeNo(paid.outTradeNo()).refundOrderNo("R11").amount(100).build()));
        assertEquals(ErrorCode.CHANNEL_INVOKE_ERROR, e.error());
        assertEquals(RefundState.FAIL, refunds.stateOf("R11"));
    }

    @Test
    void refundClampsToRefundableAmount() {
        PayResult paid = payAndSucceed("O12");
        boolean ok = flow.refund(RefundRequest.builder().channelCode(CHANNEL).orderNo("O12")
            .outTradeNo(paid.outTradeNo()).refundOrderNo("R12").amount(99900).build());
        assertTrue(ok);
        assertEquals(10000L, refunds.amountOf("R12"), "超额申请应被钳制为可退金额");
        assertEquals(0L, trades.refundableOf(paid.outTradeNo()));
    }

    @Test
    void refundOnUnpaidOrderThrows() {
        PayResult unpaid = flow.pay(payRequest("O13").build());
        ChannelException e = assertThrows(ChannelException.class,
            () -> flow.refund(RefundRequest.builder().channelCode(CHANNEL).orderNo("O13")
                .outTradeNo(unpaid.outTradeNo()).refundOrderNo("R13").amount(100).build()));
        assertEquals(ErrorCode.ORDER_NOT_PAID, e.error());
    }

    @Test
    void cancelClosesUnpaidTrade() {
        PayResult result = flow.pay(payRequest("O14").build());
        assertTrue(flow.cancel(CHANNEL, "O14", result.outTradeNo()));
        assertEquals(PayState.CLOSED, trades.stateOf(result.outTradeNo()));
    }

    @Test
    void cancelPaidTradeThrows() {
        PayResult result = flow.pay(payRequest("O15").build());
        channel.queryPaid = true;
        ChannelException e = assertThrows(ChannelException.class,
            () -> flow.cancel(CHANNEL, "O15", result.outTradeNo()));
        assertEquals(ErrorCode.ORDER_HAS_PAID, e.error());
    }

    /** 回归用例：扣款确认幂等——重复调用不重复扣款、不重复发事件 */
    @Test
    void confirmIsIdempotent() {
        PayResult result = flow.pay(payRequest("O16").build());
        ConfirmRequest request = new ConfirmRequest(CHANNEL, "O16", result.outTradeNo(), null);
        assertTrue(flow.confirm(request));
        assertTrue(flow.confirm(request), "重复调用应幂等返回成功");
        assertEquals(1, channel.confirmCount, "渠道扣款只应执行一次");
        assertEquals(1, events.paySuccess.size(), "支付成功事件只应发布一次");
        assertEquals(PayState.SUCCESS, trades.stateOf(result.outTradeNo()));
    }

    @Test
    void confirmFailureMarksTradeFail() {
        PayResult result = flow.pay(payRequest("O17").build());
        channel.confirmSuccess = false;
        ConfirmRequest request = new ConfirmRequest(CHANNEL, "O17", result.outTradeNo(), null);
        assertFalse(flow.confirm(request));
        assertEquals(PayState.FAIL, trades.stateOf(result.outTradeNo()));
        // 失败终态后再调用：幂等返回 false，不再调渠道
        assertFalse(flow.confirm(request));
        assertEquals(1, channel.confirmCount);
    }

    @Test
    void feeRuleAppliedToPayAmount() {
        ChannelServiceImpl feeKit = ChannelServiceImpl.builder()
            .addChannel(new FakeChannel(CHANNEL))
            .tradeStore(trades)
            .refundStore(refunds)
            .listener(events)
            .feeRule((channelCode, amount, currency) -> amount * 105 / 100)
            .build();
        PayResult result = feeKit.flow().pay(payRequest("O18").build());
        assertEquals(10500L, result.amount());
    }

    @Test
    void outRefundSkipsChannelInvocation() {
        PayResult paid = payAndSucceed("O19");
        boolean ok = flow.refund(RefundRequest.builder().channelCode(CHANNEL).orderNo("O19")
            .outTradeNo(paid.outTradeNo()).refundOrderNo("R19").amount(3000).outRefund(true).build());
        assertTrue(ok);
        assertEquals(0, channel.refundCount);
        assertEquals(RefundState.SUCCESS, refunds.stateOf("R19"));
    }

    @Test
    void missingTradeThrows() {
        ChannelException e = assertThrows(ChannelException.class,
            () -> flow.query(new QueryRequest(CHANNEL, "GHOST", "GHOST-T1")));
        assertEquals(ErrorCode.ORDER_DOES_NOT_EXIST, e.error());
    }

    @Test
    void kitExposesChannelCapabilities() {
        assertTrue(kit.supports(CHANNEL, ActionType.QUERY));
        assertFalse(kit.supports(PAY_ONLY, ActionType.QUERY));
        TradeInfo trade = trades.find(CHANNEL, "NONE", "NONE");
        assertNull(trade);
    }
}
