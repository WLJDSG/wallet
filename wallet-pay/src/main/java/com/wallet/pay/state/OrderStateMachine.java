package com.wallet.pay.state;
import com.wallet.common.enums.OrderEvent;
import com.wallet.common.enums.OrderState;

import com.wallet.contract.channel.error.ChannelException;
import com.wallet.common.error.ErrorCode;
import com.wallet.contract.channel.state.StateMachine;

import java.util.Map;

/**
 * 支付主单状态机。
 *
 * <pre>
 * INIT   --SUBMIT--> PAYING
 * INIT   --CLOSE--->  CLOSED
 * PAYING --FINISH--> SUCCESS
 * PAYING --FAIL---->  FAIL
 * PAYING --CLOSE--->  CLOSED
 * SUCCESS / FAIL / CLOSED 为终态，无出边。
 * </pre>
 */
public final class OrderStateMachine implements StateMachine<OrderState, OrderEvent> {

    public static final OrderStateMachine INSTANCE = new OrderStateMachine();

    private static final Map<OrderState, Map<OrderEvent, OrderState>> TABLE = Map.of(
        OrderState.INIT, Map.of(
            OrderEvent.SUBMIT, OrderState.PAYING,
            OrderEvent.CLOSE, OrderState.CLOSED),
        OrderState.PAYING, Map.of(
            OrderEvent.FINISH, OrderState.SUCCESS,
            OrderEvent.FAIL, OrderState.FAIL,
            OrderEvent.CLOSE, OrderState.CLOSED));

    private OrderStateMachine() {
    }

    @Override
    public OrderState transition(OrderState source, OrderEvent event) {
        if (!canTransition(source, event)) {
            throw new ChannelException(ErrorCode.ILLEGAL_CHANGE_STATUS, source + " -> " + event);
        }
        return TABLE.get(source).get(event);
    }

    @Override
    public boolean canTransition(OrderState source, OrderEvent event) {
        Map<OrderEvent, OrderState> edges = TABLE.get(source);
        return edges != null && edges.containsKey(event);
    }
}
