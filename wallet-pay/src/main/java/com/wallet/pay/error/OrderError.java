package com.wallet.pay.error;

import com.wallet.common.error.ErrorCode;

/**
 * 支付编排错误码。
 */
public enum OrderError implements ErrorCode {

    ORDER_NOT_FOUND("ORDER_NOT_FOUND", "支付单不存在"),
    ORDER_NOT_OWNED("ORDER_NOT_OWNED", "支付单不属于该用户"),
    ORDER_CLOSED("ORDER_CLOSED", "支付单已关闭"),
    ORDER_PAID("ORDER_PAID", "支付单已支付"),
    ORDER_STATE_INVALID("ORDER_STATE_INVALID", "支付单状态不允许该操作"),
    AMOUNT_NOT_MATCH("AMOUNT_NOT_MATCH", "分段金额之和与总额不一致"),
    PART_INVALID("PART_INVALID", "支付分段不合法"),
    TICKET_REQUIRED("TICKET_REQUIRED", "含余额/积分/券的支付需要支付密码授权票据"),
    CHANNEL_PAY_FAILED("CHANNEL_PAY_FAILED", "渠道支付发起失败"),
    REFUND_AMOUNT_INVALID("REFUND_AMOUNT_INVALID", "退款金额不合法"),
    ORDER_NOT_PAID("ORDER_NOT_PAID", "订单未支付"),
    REFUND_TOO_MUCH("REFUND_TOO_MUCH", "退款金额超出可退金额"),
    COUPON_ONLY_FULL_REFUND("COUPON_ONLY_FULL_REFUND", "优惠券仅整单退款时返还");

    private final String code;
    private final String message;

    OrderError(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String message() {
        return message;
    }
}
