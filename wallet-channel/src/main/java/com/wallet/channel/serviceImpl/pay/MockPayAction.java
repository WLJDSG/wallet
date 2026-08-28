package com.wallet.channel.serviceImpl.pay;
import com.wallet.channel.serviceImpl.support.MockPayload;
import com.wallet.channel.serviceImpl.support.MockProperties;
import com.wallet.channel.serviceImpl.support.MockStore;
import com.wallet.channel.serviceImpl.support.AbstractMockAction;

import com.wallet.contract.channel.action.PayAction;
import com.wallet.common.error.ErrorCode;
import com.wallet.contract.channel.error.ChannelException;
import com.wallet.contract.channel.model.PayRequest;
import com.wallet.contract.channel.model.TradeInfo;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * mock 支付动作（PAY）：行为由配置控制（延迟/失败率），无真实商户配置时验证编排/锁/状态机全链路。
 */
@Component
@ConditionalOnProperty(prefix = "wallet.mock", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MockPayAction extends AbstractMockAction implements PayAction {

    public MockPayAction(MockStore store, MockProperties props) {
        super(store, props);
    }

    @Override
    public Object pay(PayRequest request, TradeInfo trade) {
        sleep();
        if (Math.random() < props.getFailRate()) {
            throw new ChannelException(ErrorCode.CHANNEL_INVOKE_ERROR, "mock 渠道模拟下单失败");
        }
        store.markCreated(trade.outTradeNo());
        return new MockPayload(trade.outTradeNo(), "PAY_URL:" + trade.outTradeNo());
    }
}
