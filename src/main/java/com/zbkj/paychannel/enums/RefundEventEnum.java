package com.zbkj.paychannel.enums;

/**
 * 退款状态机事件。
 */
public enum RefundEventEnum {

    /** 发起渠道退款 */
    REFUND_REQUEST,

    /** 退款成功 */
    REFUND_SUCCESS,

    /** 退款失败 */
    REFUND_FAIL
}
