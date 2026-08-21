package com.wallet.pay.state;

/**
 * 支付主单状态机事件。
 */
public enum OrderEvent {

    /** 提交支付 */
    SUBMIT,

    /** 全部分段支付成功 */
    FINISH,

    /** 支付失败 */
    FAIL,

    /** 关闭订单 */
    CLOSE
}
