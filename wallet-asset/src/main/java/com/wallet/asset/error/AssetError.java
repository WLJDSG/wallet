package com.wallet.asset.error;

import com.wallet.common.error.ErrorCode;

/**
 * 资产模块错误码。
 */
public enum AssetError implements ErrorCode {

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
    TICKET_INVALID("TICKET_INVALID", "支付授权已失效，请重新校验");

    private final String code;
    private final String message;

    AssetError(String code, String message) {
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
