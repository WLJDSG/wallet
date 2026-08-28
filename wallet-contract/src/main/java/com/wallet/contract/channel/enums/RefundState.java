package com.wallet.contract.channel.enums;

/**
 * 渠道退款单状态。
 */
public enum RefundState {

    /** 已创建，未发起渠道退款 */
    INIT,

    /** 退款中 */
    REFUNDING,

    /** 退款成功（终态） */
    SUCCESS,

    /** 退款失败（终态） */
    FAIL
}
