package com.wallet.pay.state;

/**
 * 退款主单状态（refund_order.state，DB 存 name()）。
 */
public enum RefundOrderState {

    /** 已创建，退款处理中 */
    INIT,

    /** 三方退款已发起，等待结果（预留：当前实现三方退款同步返回） */
    REFUNDING,

    /** 退款成功（终态） */
    SUCCESS,

    /** 退款失败（终态，资产分毫未动或已按段回滚） */
    FAIL
}
