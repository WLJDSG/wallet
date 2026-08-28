package com.wallet.common.enums;

/**
 * 支付分段状态机事件。
 */
public enum PartEvent {

    /** 发起三方支付（仅三方段用） */
    START,

    /** 支付成功 */
    DONE,

    /** 支付失败 */
    FAIL,

    /** 关闭 */
    CLOSE,

    /** 补偿返还（支付未完成，把已扣资产还回去） */
    ROLLBACK
}
