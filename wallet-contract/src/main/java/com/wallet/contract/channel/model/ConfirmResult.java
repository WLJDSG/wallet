package com.wallet.contract.channel.model;

/**
 * 二段式扣款确认结果。
 *
 * @param success         扣款是否成功
 * @param thirdOutTradeNo 渠道侧交易号
 * @param failReason      失败原因（success=false 时填写）
 */
public record ConfirmResult(boolean success, String thirdOutTradeNo, String failReason) {

    public static ConfirmResult ok(String thirdOutTradeNo) {
        return new ConfirmResult(true, thirdOutTradeNo, null);
    }

    public static ConfirmResult fail(String failReason) {
        return new ConfirmResult(false, null, failReason);
    }
}
