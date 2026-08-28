package com.wallet.contract.channel.enums;

/**
 * 渠道内核错误码。code 值即宿主的 i18n 资源键，宿主捕获 {@code ChannelException}
 * 后按 {@code error().code()} 渲染本地化文案。
 */
public enum PayError {

    /** 请求参数缺失或非法 */
    PAY_PARAM_INVALID("PAY_PARAM_INVALID"),

    /** 渠道未注册，或渠道未实现该动作 */
    PAYMENT_ACTION_UNSUPPORTED("PAYMENT_ACTION_UNSUPPORTED"),

    /** 订单已支付，不可重复支付/取消 */
    ORDER_HAS_PAID("ORDER_HAS_PAID"),

    /** 交易单不存在 */
    ORDER_DOES_NOT_EXIST("ORDER_DOES_NOT_EXIST"),

    /** 订单未支付，不可退款 */
    ORDER_NOT_PAID("ORDER_NOT_PAID"),

    /** 订单已全额退款 */
    ORDER_REFUND_FINISH("ORDER_REFUND_FINISH"),

    /** 上一笔交易关闭失败，等待人工处理 */
    ORDER_PAYING_WAITE_REFUND("ORDER_PAYING_WAITE_REFUND"),

    /** 非法状态流转 */
    ILLEGAL_CHANGE_STATUS("ILLEGAL_CHANGE_STATUS"),

    /** 回调验签失败 */
    CALLBACK_VERIFY_FAILED("CALLBACK_VERIFY_FAILED"),

    /** 回调声明已支付，但主动查询结果为未支付 */
    CALLBACK_QUERY_UNPAID("CALLBACK_QUERY_UNPAID"),

    /** 退款金额非法 */
    REFUND_AMOUNT_INVALID("REFUND_AMOUNT_INVALID"),

    /** 渠道调用失败 */
    CHANNEL_INVOKE_ERROR("CHANNEL_INVOKE_ERROR");

    private final String code;

    PayError(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }
}
