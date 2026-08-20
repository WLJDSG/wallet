package com.zbkj.paychannel.core;

import com.zbkj.paychannel.PayChannelKernel;
import com.zbkj.paychannel.enums.PayErrorCode;
import com.zbkj.paychannel.enums.PayStateEnum;
import com.zbkj.paychannel.enums.RefundStateEnum;
import com.zbkj.paychannel.exception.PayChannelException;
import com.zbkj.paychannel.model.CallbackCommand;
import com.zbkj.paychannel.model.ExecutePaymentCommand;
import com.zbkj.paychannel.model.PayCommand;
import com.zbkj.paychannel.model.PayCommand.PayCommandBuilder;
import com.zbkj.paychannel.model.PayOrderSnapshot;
import com.zbkj.paychannel.model.PayResult;
import com.zbkj.paychannel.model.QueryCommand;
import com.zbkj.paychannel.model.RefundCommand;
import com.zbkj.paychannel.support.InMemoryPayOrderRepository;
import com.zbkj.paychannel.support.InMemoryRefundOrderRepository;
import com.zbkj.paychannel.support.MockChannel;
import com.zbkj.paychannel.support.PayOnlyChannel;
import com.zbkj.paychannel.support.TestDoubles;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class PaymentOrchestratorTest {

    private static final String CHANNEL = "MOCK";
    private static final String PAY_ONLY = "PAY_ONLY";

    private MockChannel channel;
    private InMemoryPayOrderRepository payOrders;
    private InMemoryRefundOrderRepository refundOrders;
    private TestDoubles.RecordingEventListener events;
    private PaymentOrchestrator orchestrator;
    private PayChannelKernel kernel;

    @Before
    public void setUp() {
        channel = new MockChannel(CHANNEL);
        payOrders = new InMemoryPayOrderRepository();
        refundOrders = new InMemoryRefundOrderRepository();
        events = new TestDoubles.RecordingEventListener();
        kernel = PayChannelKernel.builder()
            .addProvider(channel)
            .addProvider(new PayOnlyChannel(PAY_ONLY))
            .payOrderRepository(payOrders)
            .refundOrderRepository(refundOrders)
            .lockManager(new TestDoubles.DirectLockManager())
            .eventListener(events)
            .build();
        orchestrator = kernel.payment();
    }

    private PayCommandBuilder payCommand(String orderNo) {
        return PayCommand.builder().channelCode(CHANNEL).orderNo(orderNo)
            .payAmount(new BigDecimal("100")).currency("TWD");
    }

    private PayResult payAndSucceed(String orderNo) {
        PayResult result = orchestrator.pay(payCommand(orderNo).build());
        String ack = orchestrator.handleCallback(callback(orderNo, result.getOutTradeNo()));
        assertEquals("OK", ack);
        return result;
    }

    private CallbackCommand callback(String orderNo, String outTradeNo) {
        return CallbackCommand.builder().channelCode(CHANNEL).orderNo(orderNo).outTradeNo(outTradeNo)
            .body("{\"result\":\"S\"}").build();
    }

    @Test
    public void payCreatesOrderAndAdvancesToPaying() {
        PayResult result = orchestrator.pay(payCommand("O1").build());
        assertNotNull(result.getOutTradeNo());
        assertEquals(PayStateEnum.PAYING, payOrders.stateOf(result.getOutTradeNo()));
        assertTrue(result.getChannelPayload().toString().startsWith("PAY_URL:"));
        assertTrue("MOCK 渠道实现了 QueryProvider，应参与轮询", result.isQueryable());
        assertEquals(1, channel.payCount);
    }

    @Test
    public void payOnLastTradeClosesPreviousTrade() {
        PayResult first = orchestrator.pay(payCommand("O2").build());
        PayResult second = orchestrator.pay(payCommand("O2").lastOutTradeNo(first.getOutTradeNo()).build());
        assertEquals(PayStateEnum.CLOSED, payOrders.stateOf(first.getOutTradeNo()));
        assertEquals(PayStateEnum.PAYING, payOrders.stateOf(second.getOutTradeNo()));
        assertEquals(1, channel.cancelCount);
    }

    @Test
    public void payOnPaidLastTradeThrows() {
        PayResult first = orchestrator.pay(payCommand("O3").build());
        channel.queryPaid = true;
        try {
            orchestrator.pay(payCommand("O3").lastOutTradeNo(first.getOutTradeNo()).build());
            fail("上一笔已支付时应拒绝再次发起支付");
        } catch (PayChannelException e) {
            assertEquals(PayErrorCode.ORDER_HAS_PAID, e.getErrorCode());
        }
    }

    /** 回调幂等：重复回调只推进一次状态、只发布一次事件，且每次都返回渠道应答 */
    @Test
    public void callbackIsIdempotent() {
        PayResult result = orchestrator.pay(payCommand("O4").build());
        String ack1 = orchestrator.handleCallback(callback("O4", result.getOutTradeNo()));
        String ack2 = orchestrator.handleCallback(callback("O4", result.getOutTradeNo()));
        assertEquals("OK", ack1);
        assertEquals("OK", ack2);
        assertEquals(PayStateEnum.SUCCESS, payOrders.stateOf(result.getOutTradeNo()));
        assertEquals("支付成功事件应只发布一次", 1, events.paySuccess.size());
    }

    @Test
    public void callbackWithReQueryTrustsQueryResult() {
        channel.callbackReQuery = true;
        channel.queryPaid = true;
        PayResult result = orchestrator.pay(payCommand("O5").build());
        orchestrator.handleCallback(callback("O5", result.getOutTradeNo()));
        assertEquals(PayStateEnum.SUCCESS, payOrders.stateOf(result.getOutTradeNo()));
        assertEquals(1, channel.queryCount);
    }

    @Test
    public void callbackWithReQueryUnpaidThrows() {
        channel.callbackReQuery = true;
        channel.queryPaid = false;
        PayResult result = orchestrator.pay(payCommand("O6").build());
        try {
            orchestrator.handleCallback(callback("O6", result.getOutTradeNo()));
            fail("回调声明已支付但查询未支付时应抛异常");
        } catch (PayChannelException e) {
            assertEquals(PayErrorCode.CALLBACK_QUERY_UNPAID, e.getErrorCode());
        }
        assertEquals(PayStateEnum.PAYING, payOrders.stateOf(result.getOutTradeNo()));
        assertEquals(0, events.paySuccess.size());
    }

    @Test
    public void queryAdvancesToSuccessOncePaid() {
        PayResult result = orchestrator.pay(payCommand("O7").build());
        QueryCommand query = QueryCommand.builder().channelCode(CHANNEL).orderNo("O7")
            .outTradeNo(result.getOutTradeNo()).build();
        assertFalse(orchestrator.queryPayResult(query));
        channel.queryPaid = true;
        assertTrue(orchestrator.queryPayResult(query));
        assertEquals(PayStateEnum.SUCCESS, payOrders.stateOf(result.getOutTradeNo()));
        // 已终态后再查询：直接返回 true，不再调渠道
        int queriesSoFar = channel.queryCount;
        assertTrue(orchestrator.queryPayResult(query));
        assertEquals(queriesSoFar, channel.queryCount);
        assertEquals(1, events.paySuccess.size());
    }

    /** 回归用例（原模块 S2）：未实现 QUERY 的渠道，轮询调用得到明确异常而不是 NPE 死循环 */
    @Test
    public void queryOnPayOnlyChannelThrowsTypedException() {
        PayResult result = orchestrator.pay(PayCommand.builder().channelCode(PAY_ONLY).orderNo("O8")
            .payAmount(BigDecimal.TEN).currency("TWD").build());
        assertFalse("无 QueryProvider 的渠道不应参与轮询", result.isQueryable());
        try {
            orchestrator.queryPayResult(QueryCommand.builder().channelCode(PAY_ONLY).orderNo("O8")
                .outTradeNo(result.getOutTradeNo()).build());
            fail("应抛出 PAYMENT_ACTION_UNSUPPORTED");
        } catch (PayChannelException e) {
            assertEquals(PayErrorCode.PAYMENT_ACTION_UNSUPPORTED, e.getErrorCode());
        }
    }

    @Test
    public void refundSuccessReducesRefundableAndPublishesEvent() {
        PayResult paid = payAndSucceed("O9");
        boolean ok = orchestrator.refund(RefundCommand.builder().channelCode(CHANNEL).orderNo("O9")
            .outTradeNo(paid.getOutTradeNo()).refundOrderNo("R9").refundAmount(new BigDecimal("40")).build());
        assertTrue(ok);
        assertEquals(RefundStateEnum.SUCCESS, refundOrders.stateOf("R9"));
        assertEquals(new BigDecimal("60"), payOrders.refundableOf(paid.getOutTradeNo()));
        assertEquals(1, events.refundSuccess.size());
    }

    /** 回归用例（原模块 S1）：渠道拒绝退款时，退款单落 FAIL 留痕，而不是抛"非法状态变更"后无痕回滚 */
    @Test
    public void refundRejectionMarksRefundOrderAsFail() {
        PayResult paid = payAndSucceed("O10");
        channel.refundSuccess = false;
        boolean ok = orchestrator.refund(RefundCommand.builder().channelCode(CHANNEL).orderNo("O10")
            .outTradeNo(paid.getOutTradeNo()).refundOrderNo("R10").refundAmount(new BigDecimal("40")).build());
        assertFalse(ok);
        assertEquals(RefundStateEnum.FAIL, refundOrders.stateOf("R10"));
        assertEquals("可退金额不应被扣减", new BigDecimal("100"), payOrders.refundableOf(paid.getOutTradeNo()));
        assertEquals(0, events.refundSuccess.size());
    }

    /** 渠道退款抛异常同样留下 FAIL 痕迹再上抛 */
    @Test
    public void refundChannelExceptionStillMarksFail() {
        PayResult paid = payAndSucceed("O11");
        channel.refundException = new IllegalStateException("gateway timeout");
        try {
            orchestrator.refund(RefundCommand.builder().channelCode(CHANNEL).orderNo("O11")
                .outTradeNo(paid.getOutTradeNo()).refundOrderNo("R11").refundAmount(BigDecimal.ONE).build());
            fail("渠道异常应上抛");
        } catch (PayChannelException e) {
            assertEquals(PayErrorCode.CHANNEL_INVOKE_ERROR, e.getErrorCode());
        }
        assertEquals(RefundStateEnum.FAIL, refundOrders.stateOf("R11"));
    }

    @Test
    public void refundClampsToRefundableAmount() {
        PayResult paid = payAndSucceed("O12");
        boolean ok = orchestrator.refund(RefundCommand.builder().channelCode(CHANNEL).orderNo("O12")
            .outTradeNo(paid.getOutTradeNo()).refundOrderNo("R12").refundAmount(new BigDecimal("999")).build());
        assertTrue(ok);
        assertEquals("超额申请应被钳制为可退金额", new BigDecimal("100"), refundOrders.amountOf("R12"));
        assertEquals(BigDecimal.ZERO.compareTo(payOrders.refundableOf(paid.getOutTradeNo())), 0);
    }

    @Test
    public void refundOnUnpaidOrderThrows() {
        PayResult unpaid = orchestrator.pay(payCommand("O13").build());
        try {
            orchestrator.refund(RefundCommand.builder().channelCode(CHANNEL).orderNo("O13")
                .outTradeNo(unpaid.getOutTradeNo()).refundOrderNo("R13").refundAmount(BigDecimal.ONE).build());
            fail("未支付订单不可退款");
        } catch (PayChannelException e) {
            assertEquals(PayErrorCode.ORDER_NOT_PAID, e.getErrorCode());
        }
    }

    @Test
    public void cancelClosesUnpaidTrade() {
        PayResult result = orchestrator.pay(payCommand("O14").build());
        assertTrue(orchestrator.cancel(CHANNEL, "O14", result.getOutTradeNo()));
        assertEquals(PayStateEnum.CLOSED, payOrders.stateOf(result.getOutTradeNo()));
    }

    @Test
    public void cancelPaidTradeThrows() {
        PayResult result = orchestrator.pay(payCommand("O15").build());
        channel.queryPaid = true;
        try {
            orchestrator.cancel(CHANNEL, "O15", result.getOutTradeNo());
            fail("渠道侧已支付时取消应抛 ORDER_HAS_PAID");
        } catch (PayChannelException e) {
            assertEquals(PayErrorCode.ORDER_HAS_PAID, e.getErrorCode());
        }
    }

    /** 回归用例（原模块 S4）：扣款确认幂等——重复调用不重复扣款、不重复发事件 */
    @Test
    public void executePaymentIsIdempotent() {
        PayResult result = orchestrator.pay(payCommand("O16").build());
        ExecutePaymentCommand command = ExecutePaymentCommand.builder().channelCode(CHANNEL).orderNo("O16")
            .outTradeNo(result.getOutTradeNo()).build();
        assertTrue(orchestrator.executePayment(command));
        assertTrue("重复调用应幂等返回成功", orchestrator.executePayment(command));
        assertEquals("渠道扣款只应执行一次", 1, channel.executeCount);
        assertEquals("支付成功事件只应发布一次", 1, events.paySuccess.size());
        assertEquals(PayStateEnum.SUCCESS, payOrders.stateOf(result.getOutTradeNo()));
    }

    @Test
    public void executePaymentFailureMarksOrderFail() {
        PayResult result = orchestrator.pay(payCommand("O17").build());
        channel.executeSuccess = false;
        ExecutePaymentCommand command = ExecutePaymentCommand.builder().channelCode(CHANNEL).orderNo("O17")
            .outTradeNo(result.getOutTradeNo()).build();
        assertFalse(orchestrator.executePayment(command));
        assertEquals(PayStateEnum.FAIL, payOrders.stateOf(result.getOutTradeNo()));
        // 失败终态后再调用：幂等返回 false，不再调渠道
        assertFalse(orchestrator.executePayment(command));
        assertEquals(1, channel.executeCount);
    }

    @Test
    public void feePolicyAppliedToPayAmount() {
        PayChannelKernel feeKernel = PayChannelKernel.builder()
            .addProvider(new MockChannel(CHANNEL))
            .payOrderRepository(payOrders)
            .refundOrderRepository(refundOrders)
            .lockManager(new TestDoubles.DirectLockManager())
            .eventListener(events)
            .feePolicy((channelCode, amount, currency) -> amount.multiply(new BigDecimal("1.05")))
            .build();
        PayResult result = feeKernel.payment().pay(payCommand("O18").build());
        assertEquals(new BigDecimal("105.00"), result.getPayAmount());
    }

    @Test
    public void outRefundSkipsChannelInvocation() {
        PayResult paid = payAndSucceed("O19");
        boolean ok = orchestrator.refund(RefundCommand.builder().channelCode(CHANNEL).orderNo("O19")
            .outTradeNo(paid.getOutTradeNo()).refundOrderNo("R19").refundAmount(new BigDecimal("30"))
            .outRefund(true).build());
        assertTrue(ok);
        assertEquals(0, channel.refundCount);
        assertEquals(RefundStateEnum.SUCCESS, refundOrders.stateOf("R19"));
    }

    @Test
    public void findMissingOrderThrows() {
        try {
            orchestrator.queryPayResult(QueryCommand.builder().channelCode(CHANNEL).orderNo("GHOST")
                .outTradeNo("GHOST-T1").build());
            fail("不存在的交易单应抛异常");
        } catch (PayChannelException e) {
            assertEquals(PayErrorCode.ORDER_DOES_NOT_EXIST, e.getErrorCode());
        }
    }

    @Test
    public void kernelExposesChannelCapabilities() {
        assertTrue(kernel.supports(CHANNEL, com.zbkj.paychannel.enums.PayActionEnum.QUERY));
        assertFalse(kernel.supports(PAY_ONLY, com.zbkj.paychannel.enums.PayActionEnum.QUERY));
        PayOrderSnapshot snapshot = payOrders.find(CHANNEL, "NONE", "NONE");
        assertEquals(null, snapshot);
    }
}
