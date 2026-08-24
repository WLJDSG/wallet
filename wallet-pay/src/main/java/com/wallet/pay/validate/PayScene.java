package com.wallet.pay.validate;

/**
 * 支付域校验场景：一个入口一个场景，校验器声明自己适用哪些场景。
 */
public enum PayScene {

    /** 创建拆分支付单 */
    CREATE,

    /** 提交支付 */
    SUBMIT,

    /** 查详情 */
    DETAIL,

    /** 主动查证 */
    QUERY,

    /** 取消支付 */
    CANCEL,

    /** 发起退款 */
    REFUND_CREATE
}
