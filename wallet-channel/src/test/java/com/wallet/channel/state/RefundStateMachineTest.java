package com.wallet.channel.state;

import com.wallet.channel.enums.RefundEvent;
import com.wallet.channel.enums.RefundState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RefundStateMachineTest {

    private final RefundStateMachine machine = RefundStateMachine.INSTANCE;

    @Test
    void legalTransitions() {
        assertEquals(RefundState.REFUNDING,
            machine.transition(RefundState.INIT, RefundEvent.REFUND_REQUEST));
        assertEquals(RefundState.SUCCESS,
            machine.transition(RefundState.REFUNDING, RefundEvent.REFUND_SUCCESS));
        assertEquals(RefundState.FAIL,
            machine.transition(RefundState.REFUNDING, RefundEvent.REFUND_FAIL));
    }

    /** 回归用例：REFUND_FAIL 的合法源状态是 REFUNDING 而非 INIT（原 crmeb-pay-service 缺陷） */
    @Test
    void refundFailIsOnlyLegalFromRefunding() {
        assertFalse(machine.canTransition(RefundState.INIT, RefundEvent.REFUND_FAIL));
        assertTrue(machine.canTransition(RefundState.REFUNDING, RefundEvent.REFUND_FAIL));
    }

    @Test
    void terminalStatesHaveNoOutgoingEdges() {
        for (RefundState terminal : new RefundState[] {RefundState.SUCCESS, RefundState.FAIL}) {
            for (RefundEvent event : RefundEvent.values()) {
                assertFalse(machine.canTransition(terminal, event), terminal + " 不应有出边 " + event);
            }
        }
    }
}
