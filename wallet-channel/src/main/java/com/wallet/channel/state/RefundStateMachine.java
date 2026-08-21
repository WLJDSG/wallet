package com.wallet.channel.state;

import com.wallet.channel.enums.PayError;
import com.wallet.channel.enums.RefundEvent;
import com.wallet.channel.enums.RefundState;
import com.wallet.channel.error.ChannelException;

import java.util.Map;

/**
 * 退款状态机。
 *
 * <pre>
 * INIT      --REFUND_REQUEST--> REFUNDING
 * REFUNDING --REFUND_SUCCESS--> SUCCESS
 * REFUNDING --REFUND_FAIL---->  FAIL
 * SUCCESS / FAIL 为终态，无出边。
 * </pre>
 *
 * <p>注意：REFUND_FAIL 的合法源状态是 REFUNDING 而不是 INIT——
 * 编排器在渠道退款失败时必须以流转后的 REFUNDING 为源标记失败
 * （原 crmeb-pay-service 在此处误用创建时的 INIT，导致失败路径必抛非法流转）。</p>
 */
public final class RefundStateMachine implements StateMachine<RefundState, RefundEvent> {

    public static final RefundStateMachine INSTANCE = new RefundStateMachine();

    private static final Map<RefundState, Map<RefundEvent, RefundState>> TABLE = Map.of(
        RefundState.INIT, Map.of(RefundEvent.REFUND_REQUEST, RefundState.REFUNDING),
        RefundState.REFUNDING, Map.of(
            RefundEvent.REFUND_SUCCESS, RefundState.SUCCESS,
            RefundEvent.REFUND_FAIL, RefundState.FAIL));

    private RefundStateMachine() {
    }

    @Override
    public RefundState transition(RefundState source, RefundEvent event) {
        if (!canTransition(source, event)) {
            throw new ChannelException(PayError.ILLEGAL_CHANGE_STATUS, source + " -> " + event);
        }
        return TABLE.get(source).get(event);
    }

    @Override
    public boolean canTransition(RefundState source, RefundEvent event) {
        Map<RefundEvent, RefundState> edges = TABLE.get(source);
        return edges != null && edges.containsKey(event);
    }
}
