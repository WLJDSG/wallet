package com.wallet.channel.core;

import com.wallet.channel.action.QueryAction;
import com.wallet.channel.enums.ActionType;
import com.wallet.channel.enums.PayError;
import com.wallet.channel.error.ChannelException;
import com.wallet.channel.model.QueryRequest;
import com.wallet.channel.model.QueryResult;
import com.wallet.channel.support.FakeChannel;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChannelTableTest {

    @Test
    void registersAllImplementedActions() {
        ChannelTable table = new ChannelTable(List.of(new FakeChannel("MOCK")));
        for (ActionType action : ActionType.values()) {
            assertTrue(table.supports("MOCK", action), "MOCK 应支持 " + action);
        }
        assertFalse(table.supports("UNKNOWN", ActionType.PAY));
    }

    /** 回归用例：缺失组合抛类型化异常而不是 NPE（原 PaymentServiceFactory 缺陷） */
    @Test
    void missingActionThrowsTypedExceptionInsteadOfNpe() {
        ChannelTable table = new ChannelTable(List.of(new FakeChannel("MOCK")));
        ChannelException e = assertThrows(ChannelException.class,
            () -> table.require("GHOST", ActionType.QUERY));
        assertEquals(PayError.PAYMENT_ACTION_UNSUPPORTED, e.error());
        assertTrue(e.detail().contains("GHOST"));
        assertTrue(e.detail().contains("QUERY"));
    }

    @Test
    void duplicateActionRegistrationFailsAtStartup() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
            () -> new ChannelTable(List.of(new FakeChannel("MOCK"), new FakeChannel("MOCK"))));
        assertTrue(e.getMessage().contains("重复注册"));
    }

    @Test
    void channelWithoutPayActionFailsAtStartup() {
        QueryAction queryOnly = new QueryAction() {
            @Override
            public String code() {
                return "QUERY_ONLY";
            }

            @Override
            public QueryResult query(QueryRequest request) {
                return QueryResult.unpaid();
            }
        };
        IllegalStateException e = assertThrows(IllegalStateException.class,
            () -> new ChannelTable(List.of(queryOnly)));
        assertTrue(e.getMessage().contains("QUERY_ONLY"));
        assertTrue(e.getMessage().contains("PayAction"));
    }
}
