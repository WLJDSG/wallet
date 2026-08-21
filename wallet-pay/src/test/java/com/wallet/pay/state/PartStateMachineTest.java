package com.wallet.pay.state;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PartStateMachineTest {

    private final PartStateMachine machine = PartStateMachine.INSTANCE;

    @Test
    void legalTransitions() {
        assertEquals(PartState.PAYING, machine.transition(PartState.INIT, PartEvent.START));
        assertEquals(PartState.SUCCESS, machine.transition(PartState.INIT, PartEvent.DONE));
        assertEquals(PartState.SUCCESS, machine.transition(PartState.PAYING, PartEvent.DONE));
        assertEquals(PartState.FAIL, machine.transition(PartState.PAYING, PartEvent.FAIL));
        assertEquals(PartState.CLOSED, machine.transition(PartState.PAYING, PartEvent.CLOSE));
        assertEquals(PartState.ROLLBACK, machine.transition(PartState.SUCCESS, PartEvent.ROLLBACK));
    }

    @Test
    void rollbackOnlyFromSuccess() {
        assertFalse(machine.canTransition(PartState.INIT, PartEvent.ROLLBACK));
        assertFalse(machine.canTransition(PartState.PAYING, PartEvent.ROLLBACK));
        assertTrueSafe();
    }

    private void assertTrueSafe() {
        // ROLLBACK 只有 SUCCESS 源合法
        if (!machine.canTransition(PartState.SUCCESS, PartEvent.ROLLBACK)) {
            throw new AssertionError("SUCCESS --ROLLBACK--> ROLLBACK 应合法");
        }
    }

    @Test
    void terminalHasNoOutgoingEdge() {
        for (PartState terminal : new PartState[] {PartState.FAIL, PartState.CLOSED, PartState.ROLLBACK}) {
            for (PartEvent event : PartEvent.values()) {
                assertFalse(machine.canTransition(terminal, event), terminal + " 不应有出边 " + event);
            }
        }
    }

    @Test
    void illegalTransitionThrows() {
        assertThrows(RuntimeException.class,
            () -> machine.transition(PartState.ROLLBACK, PartEvent.DONE));
    }
}
