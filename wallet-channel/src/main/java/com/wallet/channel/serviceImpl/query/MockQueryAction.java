package com.wallet.channel.serviceImpl.query;
import com.wallet.channel.serviceImpl.support.MockProperties;
import com.wallet.channel.serviceImpl.support.MockStore;
import com.wallet.channel.serviceImpl.support.AbstractMockAction;

import com.wallet.contract.channel.action.QueryAction;
import com.wallet.contract.channel.model.QueryRequest;
import com.wallet.contract.channel.model.QueryResult;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * mock 查询动作（QUERY）。
 */
@Component
@ConditionalOnProperty(prefix = "wallet.mock", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MockQueryAction extends AbstractMockAction implements QueryAction {

    public MockQueryAction(MockStore store, MockProperties props) {
        super(store, props);
    }

    @Override
    public QueryResult query(QueryRequest request) {
        sleep();
        boolean paid = store.isPaid(request.outTradeNo());
        return new QueryResult(paid, paid ? "MOCK-TXN-" + request.outTradeNo() : null);
    }
}
