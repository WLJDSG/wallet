package com.wallet.common.error;

/**
 * 统一错误码（全量）：通用 + 账户/资产 + 支付编排 + 渠道内核，code 全局唯一，message 为默认文案。
 *
 * <p>业务异常走 {@link CommonException}（接口层统一转 ApiResult）；渠道内核抛
 * {@code ChannelException}（宿主按 code 渲染 i18n，message 为兜底默认文案）。</p>
 */
public enum ErrorCode {

    // ===== 通用 =====
    BAD_PARAM("BAD_PARAM", "参数不正确"),
    LOCK_FAILED("LOCK_FAILED", "操作太频繁，请稍后再试"),
    RATE_LIMITED("RATE_LIMITED", "请求过于频繁，请稍后再试"),
    DATA_NOT_FOUND("DATA_NOT_FOUND", "数据不存在"),
    SYSTEM_ERROR("SYSTEM_ERROR", "系统繁忙，请稍后再试"),

    // ===== 账户/资产 =====
    ACCOUNT_NOT_FOUND("ACCOUNT_NOT_FOUND", "账户不存在"),
    MONEY_NOT_ENOUGH("MONEY_NOT_ENOUGH", "余额不足"),
    POINT_NOT_ENOUGH("POINT_NOT_ENOUGH", "积分不足"),
    COUPON_NOT_EXIST("COUPON_NOT_EXIST", "优惠券不存在"),
    COUPON_NOT_OWNED("COUPON_NOT_OWNED", "优惠券不属于该用户"),
    COUPON_USED("COUPON_USED", "优惠券已被使用"),
    COUPON_EXPIRED("COUPON_EXPIRED", "优惠券已过期"),
    COUPON_NOT_MATCH("COUPON_NOT_MATCH", "优惠券不满足使用条件"),
    COUPON_SOLD_OUT("COUPON_SOLD_OUT", "优惠券已领完"),
    PASSWORD_NOT_SET("PASSWORD_NOT_SET", "尚未设置支付密码"),
    PASSWORD_WRONG("PASSWORD_WRONG", "支付密码错误"),
    PASSWORD_LOCKED("PASSWORD_LOCKED", "错误次数过多，支付密码已锁定"),
    PASSWORD_EXISTS("PASSWORD_EXISTS", "已设置支付密码"),
    TICKET_INVALID("TICKET_INVALID", "支付授权已失效，请重新校验"),

    // ===== 支付编排 =====
    ORDER_NOT_FOUND("ORDER_NOT_FOUND", "支付单不存在"),
    ORDER_NOT_OWNED("ORDER_NOT_OWNED", "支付单不属于该用户"),
    ORDER_CLOSED("ORDER_CLOSED", "支付单已关闭"),
    ORDER_PAID("ORDER_PAID", "支付单已支付"),
    ORDER_STATE_INVALID("ORDER_STATE_INVALID", "支付单状态不允许该操作"),
    AMOUNT_NOT_MATCH("AMOUNT_NOT_MATCH", "分段金额之和与总额不一致"),
    PART_INVALID("PART_INVALID", "支付分段不合法"),
    TICKET_REQUIRED("TICKET_REQUIRED", "含余额/积分/券的支付需要支付密码授权票据"),
    CHANNEL_PAY_FAILED("CHANNEL_PAY_FAILED", "渠道支付发起失败"),
    REFUND_TOO_MUCH("REFUND_TOO_MUCH", "退款金额超出可退金额"),
    COUPON_ONLY_FULL_REFUND("COUPON_ONLY_FULL_REFUND", "优惠券仅整单退款时返还"),
    ORDER_NOT_PAID("ORDER_NOT_PAID", "订单未支付"),
    REFUND_AMOUNT_INVALID("REFUND_AMOUNT_INVALID", "退款金额不合法"),

    // ===== 渠道内核 =====
    PAY_PARAM_INVALID("PAY_PARAM_INVALID", "渠道请求参数缺失或非法"),
    PAYMENT_ACTION_UNSUPPORTED("PAYMENT_ACTION_UNSUPPORTED", "渠道未注册或未实现该动作"),
    ORDER_HAS_PAID("ORDER_HAS_PAID", "订单已支付，不可重复支付/取消"),
    ORDER_DOES_NOT_EXIST("ORDER_DOES_NOT_EXIST", "交易单不存在"),
    ORDER_REFUND_FINISH("ORDER_REFUND_FINISH", "订单已全额退款"),
    ORDER_PAYING_WAITE_REFUND("ORDER_PAYING_WAITE_REFUND", "上一笔交易未完成，等待人工处理"),
    ILLEGAL_CHANGE_STATUS("ILLEGAL_CHANGE_STATUS", "非法的状态流转"),
    CALLBACK_VERIFY_FAILED("CALLBACK_VERIFY_FAILED", "回调验签失败"),
    CALLBACK_QUERY_UNPAID("CALLBACK_QUERY_UNPAID", "回调声明已支付，但主动查询结果为未支付"),
    CHANNEL_INVOKE_ERROR("CHANNEL_INVOKE_ERROR", "渠道调用失败");

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String code() {
        return code;
    }

    public String message() {
        return message;
    }
}
