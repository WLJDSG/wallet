package com.wallet.channel.state;

import com.wallet.contract.channel.enums.PayError;
import com.wallet.channel.enums.PayEvent;
import com.wallet.contract.channel.enums.PayState;
import com.wallet.contract.channel.error.ChannelException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PayStateMachineTest {

    private final PayStateMachine machine = PayStateMachine.INSTANCE;

    @Test
    void legalTransitions() {
        assertEquals(PayState.PAYING, machine.transition(PayState.INIT, PayEvent.PAY_REQUEST));
        assertEquals(PayState.CLOSED, machine.transition(PayState.INIT, PayEvent.CLOSE));
        assertEquals(PayState.SUCCESS, machine.transition(PayState.PAYING, PayEvent.PAY_SUCCESS));
        assertEquals(PayState.FAIL, machine.transition(PayState.PAYING, PayEvent.PAY_FAIL));
        assertEquals(PayState.CLOSED, machine.transition(PayState.PAYING, PayEvent.CLOSE));
    }

    @Test
    void terminalStatesHaveNoOutgoingEdges() {
        for (PayState terminal : new PayState[] {PayState.SUCCESS, PayState.FAIL, PayState.CLOSED}) {
            for (PayEvent event : PayEvent.values()) {
                assertFalse(machine.canTransition(terminal, event), terminal + " 不应有出边 " + event);
            }
        }
    }

    @Test
    void illegalTransitionThrowsTypedException() {
        ChannelException e = assertThrows(ChannelException.class,
            () -> machine.transition(PayState.SUCCESS, PayEvent.PAY_SUCCESS));
        assertEquals(PayError.ILLEGAL_CHANGE_STATUS, e.error());
        assertTrue(e.detail().contains("SUCCESS"));
    }
}
