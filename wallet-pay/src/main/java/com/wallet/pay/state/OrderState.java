package com.wallet.pay.state;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 支付主单状态。
 */
@Schema(description = "支付主单状态")
public enum OrderState {

    /** 已创建，未提交支付 */
    INIT,

    /** 已提交，部分/全部分段支付中（等三方回调或超时关单） */
    PAYING,

    /** 支付成功（终态） */
    SUCCESS,

    /** 支付失败（终态） */
    FAIL,

    /** 已关闭（终态） */
    CLOSED
}
