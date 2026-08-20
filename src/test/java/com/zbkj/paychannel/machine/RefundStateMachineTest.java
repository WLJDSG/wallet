package com.zbkj.paychannel.machine;

import com.zbkj.paychannel.enums.RefundEventEnum;
import com.zbkj.paychannel.enums.RefundStateEnum;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RefundStateMachineTest {

    private final RefundStateMachine machine = RefundStateMachine.INSTANCE;

    @Test
    public void legalTransitions() {
        assertEquals(RefundStateEnum.REFUNDING,
            machine.transition(RefundStateEnum.INIT, RefundEventEnum.REFUND_REQUEST));
        assertEquals(RefundStateEnum.SUCCESS,
            machine.transition(RefundStateEnum.REFUNDING, RefundEventEnum.REFUND_SUCCESS));
        assertEquals(RefundStateEnum.FAIL,
            machine.transition(RefundStateEnum.REFUNDING, RefundEventEnum.REFUND_FAIL));
    }

    /** 回归用例：REFUND_FAIL 的合法源状态是 REFUNDING 而非 INIT（原 crmeb-pay-service 缺陷） */
    @Test
    public void refundFailIsOnlyLegalFromRefunding() {
        assertFalse(machine.canTransition(RefundStateEnum.INIT, RefundEventEnum.REFUND_FAIL));
        assertTrue(machine.canTransition(RefundStateEnum.REFUNDING, RefundEventEnum.REFUND_FAIL));
    }

    @Test
    public void terminalStatesHaveNoOutgoingEdges() {
        for (RefundStateEnum terminal : new RefundStateEnum[] {RefundStateEnum.SUCCESS, RefundStateEnum.FAIL}) {
            for (RefundEventEnum event : RefundEventEnum.values()) {
                assertFalse(terminal + " 不应有出边 " + event, machine.canTransition(terminal, event));
            }
        }
    }
}
