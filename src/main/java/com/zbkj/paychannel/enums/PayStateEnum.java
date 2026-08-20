package com.zbkj.paychannel.enums;

/**
 * 支付单状态。名称与 CRMEB 宿主的 PayStatusEnum 保持一致，便于存量数据直接映射。
 */
public enum PayStateEnum {

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
