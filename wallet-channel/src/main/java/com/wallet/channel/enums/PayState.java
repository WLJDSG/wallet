package com.wallet.channel.enums;

/**
 * 渠道交易单状态（在钱包工程里对应 pay_part 中三方分段的状态）。
 */
public enum PayState {

    /** 已创建，未发起渠道支付 */
    INIT,

    /** 已发起渠道支付，等待结果 */
    PAYING,

    /** 支付成功（终态） */
    SUCCESS,

    /** 支付失败（终态） */
    FAIL,

    /** 已关闭（终态） */
    CLOSED
}
