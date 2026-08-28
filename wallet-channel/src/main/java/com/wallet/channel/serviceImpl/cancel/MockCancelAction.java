package com.wallet.channel.serviceImpl.cancel;
import com.wallet.channel.serviceImpl.support.MockProperties;
import com.wallet.channel.serviceImpl.support.MockStore;
import com.wallet.channel.serviceImpl.support.AbstractMockAction;

import com.wallet.contract.channel.action.CancelAction;
import com.wallet.contract.channel.model.CancelRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * mock 关单动作（CANCEL）。
 */
@Component
@ConditionalOnProperty(prefix = "wallet.mock", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MockCancelAction extends AbstractMockAction implements CancelAction {

    public MockCancelAction(MockStore store, MockProperties props) {
        super(store, props);
    }

    @Override
    public boolean cancel(CancelRequest request) {
        sleep();
        store.markCancelled(request.outTradeNo());
        return true;
    }
}
