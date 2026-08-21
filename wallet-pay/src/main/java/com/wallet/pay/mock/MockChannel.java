package com.wallet.pay.mock;

import com.wallet.channel.action.CallbackAction;
import com.wallet.channel.action.CancelAction;
import com.wallet.channel.action.PayAction;
import com.wallet.channel.action.QueryAction;
import com.wallet.channel.action.RefundAction;
import com.wallet.channel.enums.PayError;
import com.wallet.channel.error.ChannelException;
import com.wallet.channel.model.CallbackRequest;
import com.wallet.channel.model.CallbackResult;
import com.wallet.channel.model.CancelRequest;
import com.wallet.channel.model.ChannelRefundRequest;
import com.wallet.channel.model.PayRequest;
import com.wallet.channel.model.QueryRequest;
import com.wallet.channel.model.QueryResult;
import com.wallet.channel.model.RefundResult;
import com.wallet.channel.model.TradeInfo;
import com.wallet.pay.config.MockProperties;
import org.springframework.stereotype.Component;

/**
 * 模拟渠道：覆盖全部动作，行为由配置控制（延迟/失败率/自动回调/退款强制失败）。
 * 用于在无真实商户配置时验证编排、锁、状态机全链路。
 */
@Component
public class MockChannel implements PayAction, QueryAction, RefundAction, CancelAction, CallbackAction {

    private final MockStore store;
    private final MockProperties props;

    public MockChannel(MockStore store, MockProperties props) {
        this.store = store;
        this.props = props;
    }

    @Override
    public String code() {
        return "MOCK";
    }

    @Override
    public Object pay(PayRequest request, TradeInfo trade) {
        sleep();
        if (Math.random() < props.getFailRate()) {
            throw new ChannelException(PayError.CHANNEL_INVOKE_ERROR, "mock 渠道模拟下单失败");
        }
        store.markCreated(trade.outTradeNo());
        return new MockPayload(trade.outTradeNo(), "PAY_URL:" + trade.outTradeNo());
    }

    @Override
    public QueryResult query(QueryRequest request) {
        sleep();
        boolean paid = store.isPaid(request.outTradeNo());
        return new QueryResult(paid, paid ? "MOCK-TXN-" + request.outTradeNo() : null);
    }

    @Override
    public RefundResult refund(ChannelRefundRequest request) {
        sleep();
        if (props.isRefundFail()) {
            return RefundResult.fail("mock 渠道模拟退款失败");
        }
        store.markRefunded(request.outTradeNo());
        return RefundResult.ok("MOCK-REF-" + request.refundOrderNo());
    }

    @Override
    public boolean cancel(CancelRequest request) {
        sleep();
        store.markCancelled(request.outTradeNo());
        return true;
    }

    @Override
    public CallbackResult onCallback(CallbackRequest request) {
        // 验签：mock 用 header X-Mock-Token 与配置 secret 比对
        String token = request.headers() == null ? null : request.headers().get("x-mock-token");
        if (!props.getSecret().equals(token)) {
            throw new ChannelException(PayError.CALLBACK_VERIFY_FAILED, "mock 回调签名校验失败");
        }
        if (request.body() == null || !request.body().contains("\"result\":\"SUCCESS\"")) {
            return CallbackResult.builder().paid(false).ackBody("{\"code\":\"FAIL\"}").build();
        }
        store.markPaid(request.outTradeNo());
        return CallbackResult.builder()
            .paid(true)
            .thirdOutTradeNo("MOCK-TXN-" + request.outTradeNo())
            .ackBody("{\"code\":\"SUCCESS\"}")
            .build();
    }

    private void sleep() {
        if (props.getDelayMs() > 0) {
            try {
                Thread.sleep(props.getDelayMs());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
