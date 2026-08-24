package com.wallet.pay.state;

/**
 * 支付分段状态。
 */
public enum PartState {

    /** 已创建，未处理 */
    INIT,

    /** 三方段已发起渠道支付，等结果 */
    PAYING,

    /** 支付成功（终态） */
    SUCCESS,

    /** 支付失败（终态） */
    FAIL,

    /** 已关闭（终态） */
    CLOSED,

    /** 支付未完成时的补偿返还（终态，区别于退款） */
    ROLLBACK;

    /** 是否终态（不可再推进） */
    public boolean isTerminal() {
        return this == SUCCESS || this == FAIL || this == CLOSED || this == ROLLBACK;
    }
}
