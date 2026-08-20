package com.zbkj.paychannel.machine;

import com.zbkj.paychannel.enums.PayErrorCode;
import com.zbkj.paychannel.enums.PayEventEnum;
import com.zbkj.paychannel.enums.PayStateEnum;
import com.zbkj.paychannel.exception.PayChannelException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class PayStateMachineTest {

    private final PayStateMachine machine = PayStateMachine.INSTANCE;

    @Test
    public void legalTransitions() {
        assertEquals(PayStateEnum.PAYING, machine.transition(PayStateEnum.INIT, PayEventEnum.PAY_REQUEST));
        assertEquals(PayStateEnum.CLOSED, machine.transition(PayStateEnum.INIT, PayEventEnum.CLOSE));
        assertEquals(PayStateEnum.SUCCESS, machine.transition(PayStateEnum.PAYING, PayEventEnum.PAY_SUCCESS));
        assertEquals(PayStateEnum.FAIL, machine.transition(PayStateEnum.PAYING, PayEventEnum.PAY_FAIL));
        assertEquals(PayStateEnum.CLOSED, machine.transition(PayStateEnum.PAYING, PayEventEnum.CLOSE));
    }

    @Test
    public void terminalStatesHaveNoOutgoingEdges() {
        for (PayStateEnum terminal : new PayStateEnum[] {PayStateEnum.SUCCESS, PayStateEnum.FAIL,
            PayStateEnum.CLOSED}) {
            for (PayEventEnum event : PayEventEnum.values()) {
                assertFalse(terminal + " 不应有出边 " + event, machine.canTransition(terminal, event));
            }
        }
    }

    @Test
    public void illegalTransitionThrowsTypedException() {
        try {
            machine.transition(PayStateEnum.SUCCESS, PayEventEnum.PAY_SUCCESS);
            fail("应抛出非法流转异常");
        } catch (PayChannelException e) {
            assertEquals(PayErrorCode.ILLEGAL_CHANGE_STATUS, e.getErrorCode());
            assertTrue(e.getDetail().contains("SUCCESS"));
        }
    }
}
