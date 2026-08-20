package com.zbkj.paychannel.enums;

/**
 * 支付状态机事件。
 */
public enum PayEventEnum {

    /** 发起渠道支付 */
    PAY_REQUEST,

    /** 支付成功（回调/查询/扣款确认） */
    PAY_SUCCESS,

    /** 支付失败 */
    PAY_FAIL,

    /** 关闭交易 */
    CLOSE
}
