package com.wallet.pay.state;

import com.wallet.channel.error.ChannelException;
import com.wallet.channel.enums.PayError;
import com.wallet.channel.state.StateMachine;

import java.util.Map;

/**
 * 支付分段状态机。
 *
 * <pre>
 * INIT    --START----> PAYING   （三方段发起渠道支付）
 * INIT    --DONE-----> SUCCESS  （资产段同步扣成）
 * INIT    --FAIL-----> FAIL
 * INIT    --CLOSE----> CLOSED
 * PAYING  --DONE-----> SUCCESS  （回调/查询/扣款确认）
 * PAYING  --FAIL-----> FAIL
 * PAYING  --CLOSE----> CLOSED
 * SUCCESS --ROLLBACK-> ROLLBACK （支付未完成时的补偿返还）
 * FAIL / CLOSED / ROLLBACK 为终态，无出边。
 * </pre>
 */
public final class PartStateMachine implements StateMachine<PartState, PartEvent> {

    public static final PartStateMachine INSTANCE = new PartStateMachine();

    private static final Map<PartState, Map<PartEvent, PartState>> TABLE = Map.of(
        PartState.INIT, Map.of(
            PartEvent.START, PartState.PAYING,
            PartEvent.DONE, PartState.SUCCESS,
            PartEvent.FAIL, PartState.FAIL,
            PartEvent.CLOSE, PartState.CLOSED),
        PartState.PAYING, Map.of(
            PartEvent.DONE, PartState.SUCCESS,
            PartEvent.FAIL, PartState.FAIL,
            PartEvent.CLOSE, PartState.CLOSED),
        PartState.SUCCESS, Map.of(PartEvent.ROLLBACK, PartState.ROLLBACK));

    private PartStateMachine() {
    }

    @Override
    public PartState transition(PartState source, PartEvent event) {
        if (!canTransition(source, event)) {
            throw new ChannelException(PayError.ILLEGAL_CHANGE_STATUS, source + " -> " + event);
        }
        return TABLE.get(source).get(event);
    }

    @Override
    public boolean canTransition(PartState source, PartEvent event) {
        Map<PartEvent, PartState> edges = TABLE.get(source);
        return edges != null && edges.containsKey(event);
    }
}
