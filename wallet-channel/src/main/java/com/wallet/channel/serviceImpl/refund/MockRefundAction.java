package com.wallet.channel.serviceImpl.refund;
import com.wallet.channel.serviceImpl.support.MockProperties;
import com.wallet.channel.serviceImpl.support.MockStore;
import com.wallet.channel.serviceImpl.support.AbstractMockAction;

import com.wallet.contract.channel.action.RefundAction;
import com.wallet.contract.channel.model.ChannelRefundRequest;
import com.wallet.contract.channel.model.RefundResult;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * mock 退款动作（REFUND）：可配置强制失败演练退款 FAIL 路径。
 */
@Component
@ConditionalOnProperty(prefix = "wallet.mock", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MockRefundAction extends AbstractMockAction implements RefundAction {

    public MockRefundAction(MockStore store, MockProperties props) {
        super(store, props);
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
}
