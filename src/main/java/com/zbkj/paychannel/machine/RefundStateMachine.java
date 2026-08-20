package com.zbkj.paychannel.machine;

import com.zbkj.paychannel.enums.PayErrorCode;
import com.zbkj.paychannel.enums.RefundEventEnum;
import com.zbkj.paychannel.enums.RefundStateEnum;
import com.zbkj.paychannel.exception.PayChannelException;

import java.util.Collections;
import java.util.EnumMap;
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
public final class RefundStateMachine implements StateMachine<RefundStateEnum, RefundEventEnum> {

    public static final RefundStateMachine INSTANCE = new RefundStateMachine();

    private static final Map<RefundStateEnum, Map<RefundEventEnum, RefundStateEnum>> TRANSITIONS;

    static {
        Map<RefundStateEnum, Map<RefundEventEnum, RefundStateEnum>> table = new EnumMap<>(RefundStateEnum.class);

        Map<RefundEventEnum, RefundStateEnum> init = new EnumMap<>(RefundEventEnum.class);
        init.put(RefundEventEnum.REFUND_REQUEST, RefundStateEnum.REFUNDING);
        table.put(RefundStateEnum.INIT, Collections.unmodifiableMap(init));

        Map<RefundEventEnum, RefundStateEnum> refunding = new EnumMap<>(RefundEventEnum.class);
        refunding.put(RefundEventEnum.REFUND_SUCCESS, RefundStateEnum.SUCCESS);
        refunding.put(RefundEventEnum.REFUND_FAIL, RefundStateEnum.FAIL);
        table.put(RefundStateEnum.REFUNDING, Collections.unmodifiableMap(refunding));

        TRANSITIONS = Collections.unmodifiableMap(table);
    }

    private RefundStateMachine() {
    }

    @Override
    public RefundStateEnum transition(RefundStateEnum source, RefundEventEnum event) {
        if (!canTransition(source, event)) {
            throw new PayChannelException(PayErrorCode.ILLEGAL_CHANGE_STATUS, source + " -> " + event);
        }
        return TRANSITIONS.get(source).get(event);
    }

    @Override
    public boolean canTransition(RefundStateEnum source, RefundEventEnum event) {
        Map<RefundEventEnum, RefundStateEnum> transitions = TRANSITIONS.get(source);
        return transitions != null && transitions.containsKey(event);
    }
}
