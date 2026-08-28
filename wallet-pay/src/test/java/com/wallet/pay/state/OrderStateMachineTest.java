package com.wallet.pay.state;
import com.wallet.common.enums.OrderEvent;
import com.wallet.common.enums.OrderState;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderStateMachineTest {

    private final OrderStateMachine machine = OrderStateMachine.INSTANCE;

    @Test
    void legalTransitions() {
        assertEquals(OrderState.PAYING, machine.transition(OrderState.INIT, OrderEvent.SUBMIT));
        assertEquals(OrderState.CLOSED, machine.transition(OrderState.INIT, OrderEvent.CLOSE));
        assertEquals(OrderState.SUCCESS, machine.transition(OrderState.PAYING, OrderEvent.FINISH));
        assertEquals(OrderState.FAIL, machine.transition(OrderState.PAYING, OrderEvent.FAIL));
        assertEquals(OrderState.CLOSED, machine.transition(OrderState.PAYING, OrderEvent.CLOSE));
    }

    @Test
    void terminalHasNoOutgoingEdge() {
        for (OrderState terminal : new OrderState[] {OrderState.SUCCESS, OrderState.FAIL, OrderState.CLOSED}) {
            for (OrderEvent event : OrderEvent.values()) {
                assertFalse(machine.canTransition(terminal, event), terminal + " 不应有出边 " + event);
            }
        }
    }

    @Test
    void illegalTransitionThrows() {
        assertThrows(RuntimeException.class,
            () -> machine.transition(OrderState.SUCCESS, OrderEvent.SUBMIT));
    }
}
