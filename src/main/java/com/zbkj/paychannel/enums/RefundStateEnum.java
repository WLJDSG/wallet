package com.zbkj.paychannel.enums;

/**
 * 退款单状态。名称与 CRMEB 宿主的 RefundStatusEnum 保持一致。
 */
public enum RefundStateEnum {

    /** 已创建，未发起渠道退款 */
    INIT,

    /** 退款中 */
    REFUNDING,

    /** 退款成功（终态） */
    SUCCESS,

    /** 退款失败（终态） */
    FAIL
}
