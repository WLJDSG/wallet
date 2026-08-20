package com.zbkj.paychannel.core;

import com.zbkj.paychannel.enums.PayActionEnum;
import com.zbkj.paychannel.enums.PayErrorCode;
import com.zbkj.paychannel.exception.PayChannelException;
import com.zbkj.paychannel.model.QueryCommand;
import com.zbkj.paychannel.model.QueryResult;
import com.zbkj.paychannel.provider.QueryProvider;
import com.zbkj.paychannel.support.MockChannel;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ProviderRegistryTest {

    @Test
    public void registersAllImplementedActions() {
        ProviderRegistry registry = new ProviderRegistry(Collections.singletonList(new MockChannel("MOCK")));
        for (PayActionEnum action : PayActionEnum.values()) {
            assertTrue("MOCK 应支持 " + action, registry.supports("MOCK", action));
        }
        assertFalse(registry.supports("UNKNOWN", PayActionEnum.PAY));
    }

    /** 回归用例：缺失组合抛类型化异常而不是 NPE（原 PaymentServiceFactory 缺陷） */
    @Test
    public void missingActionThrowsTypedExceptionInsteadOfNpe() {
        ProviderRegistry registry = new ProviderRegistry(Collections.singletonList(new MockChannel("MOCK")));
        try {
            registry.require("GHOST", PayActionEnum.QUERY);
            fail("应抛出 PAYMENT_ACTION_UNSUPPORTED");
        } catch (PayChannelException e) {
            assertEquals(PayErrorCode.PAYMENT_ACTION_UNSUPPORTED, e.getErrorCode());
            assertTrue(e.getDetail().contains("GHOST"));
            assertTrue(e.getDetail().contains("QUERY"));
        }
    }

    @Test
    public void duplicateActionRegistrationFailsAtStartup() {
        try {
            new ProviderRegistry(Arrays.asList(new MockChannel("MOCK"), new MockChannel("MOCK")));
            fail("重复注册应在构建期失败");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("重复注册"));
        }
    }

    @Test
    public void channelWithoutPayProviderFailsAtStartup() {
        QueryProvider queryOnly = new QueryProvider() {
            @Override
            public String channelCode() {
                return "QUERY_ONLY";
            }

            @Override
            public QueryResult query(QueryCommand command) {
                return QueryResult.builder().paid(false).build();
            }
        };
        try {
            new ProviderRegistry(Collections.singletonList(queryOnly));
            fail("缺 PAY 动作的渠道应在构建期失败");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("QUERY_ONLY"));
            assertTrue(e.getMessage().contains("PayProvider"));
        }
    }
}
