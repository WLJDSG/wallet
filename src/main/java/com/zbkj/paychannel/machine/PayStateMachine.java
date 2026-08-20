package com.zbkj.paychannel.machine;

import com.zbkj.paychannel.enums.PayErrorCode;
import com.zbkj.paychannel.enums.PayEventEnum;
import com.zbkj.paychannel.enums.PayStateEnum;
import com.zbkj.paychannel.exception.PayChannelException;

import java.util.Collections;
import java.util.EnumMap;
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
public final class PayStateMachine implements StateMachine<PayStateEnum, PayEventEnum> {

    public static final PayStateMachine INSTANCE = new PayStateMachine();

    private static final Map<PayStateEnum, Map<PayEventEnum, PayStateEnum>> TRANSITIONS;

    static {
        Map<PayStateEnum, Map<PayEventEnum, PayStateEnum>> table = new EnumMap<>(PayStateEnum.class);

        Map<PayEventEnum, PayStateEnum> init = new EnumMap<>(PayEventEnum.class);
        init.put(PayEventEnum.PAY_REQUEST, PayStateEnum.PAYING);
        init.put(PayEventEnum.CLOSE, PayStateEnum.CLOSED);
        table.put(PayStateEnum.INIT, Collections.unmodifiableMap(init));

        Map<PayEventEnum, PayStateEnum> paying = new EnumMap<>(PayEventEnum.class);
        paying.put(PayEventEnum.PAY_SUCCESS, PayStateEnum.SUCCESS);
        paying.put(PayEventEnum.PAY_FAIL, PayStateEnum.FAIL);
        paying.put(PayEventEnum.CLOSE, PayStateEnum.CLOSED);
        table.put(PayStateEnum.PAYING, Collections.unmodifiableMap(paying));

        TRANSITIONS = Collections.unmodifiableMap(table);
    }

    private PayStateMachine() {
    }

    @Override
    public PayStateEnum transition(PayStateEnum source, PayEventEnum event) {
        if (!canTransition(source, event)) {
            throw new PayChannelException(PayErrorCode.ILLEGAL_CHANGE_STATUS, source + " -> " + event);
        }
        return TRANSITIONS.get(source).get(event);
    }

    @Override
    public boolean canTransition(PayStateEnum source, PayEventEnum event) {
        Map<PayEventEnum, PayStateEnum> transitions = TRANSITIONS.get(source);
        return transitions != null && transitions.containsKey(event);
    }
}
