package com.wallet.channel.state;

import com.wallet.channel.enums.PayError;
import com.wallet.channel.enums.PayEvent;
import com.wallet.channel.enums.PayState;
import com.wallet.channel.error.ChannelException;

import java.util.Map;

/**
 * 支付状态机。
 *
 * <pre>
 * INIT   --PAY_REQUEST--> PAYING
 * INIT   --CLOSE-------->  CLOSED
 * PAYING --PAY_SUCCESS--> SUCCESS
 * PAYING --PAY_FAIL----->  FAIL
 * PAYING --CLOSE-------->  CLOSED
 * SUCCESS / FAIL / CLOSED 为终态，无出边。
 * </pre>
 */
public final class PayStateMachine implements StateMachine<PayState, PayEvent> {

    public static final PayStateMachine INSTANCE = new PayStateMachine();

    private static final Map<PayState, Map<PayEvent, PayState>> TABLE = Map.of(
        PayState.INIT, Map.of(
            PayEvent.PAY_REQUEST, PayState.PAYING,
            PayEvent.CLOSE, PayState.CLOSED),
        PayState.PAYING, Map.of(
            PayEvent.PAY_SUCCESS, PayState.SUCCESS,
            PayEvent.PAY_FAIL, PayState.FAIL,
            PayEvent.CLOSE, PayState.CLOSED));

    private PayStateMachine() {
    }

    @Override
    public PayState transition(PayState source, PayEvent event) {
        if (!canTransition(source, event)) {
            throw new ChannelException(PayError.ILLEGAL_CHANGE_STATUS, source + " -> " + event);
        }
        return TABLE.get(source).get(event);
    }

    @Override
    public boolean canTransition(PayState source, PayEvent event) {
        Map<PayEvent, PayState> edges = TABLE.get(source);
        return edges != null && edges.containsKey(event);
    }
}
