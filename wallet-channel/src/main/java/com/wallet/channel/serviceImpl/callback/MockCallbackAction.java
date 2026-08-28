package com.wallet.channel.serviceImpl.callback;
import com.wallet.channel.serviceImpl.support.MockProperties;
import com.wallet.channel.serviceImpl.support.MockStore;
import com.wallet.channel.serviceImpl.support.AbstractMockAction;

import com.wallet.contract.channel.action.CallbackAction;
import com.wallet.common.error.ErrorCode;
import com.wallet.contract.channel.error.ChannelException;
import com.wallet.contract.channel.model.CallbackRequest;
import com.wallet.contract.channel.model.CallbackResult;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * mock 回调动作（CALLBACK）：用 header X-Mock-Token 与配置 secret 验签。
 */
@Component
@ConditionalOnProperty(prefix = "wallet.mock", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MockCallbackAction extends AbstractMockAction implements CallbackAction {

    public MockCallbackAction(MockStore store, MockProperties props) {
        super(store, props);
    }

    @Override
    public CallbackResult onCallback(CallbackRequest request) {
        // 验签：mock 用 header X-Mock-Token 与配置 secret 比对
        String token = request.headers() == null ? null : request.headers().get("x-mock-token");
        if (!props.getSecret().equals(token)) {
            throw new ChannelException(ErrorCode.CALLBACK_VERIFY_FAILED, "mock 回调签名校验失败");
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
}
