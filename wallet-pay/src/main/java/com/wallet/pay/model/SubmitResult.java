package com.wallet.pay.model;

/**
 * 提交支付结果。
 *
 * @param state          主单状态：SUCCESS 当场完成 / PAYING 已发起等回调
 * @param channelPayload 渠道支付参数（三方向前端展示），纯资产支付为 null
 * @param message        提示
 */
public record SubmitResult(String state, Object channelPayload, String message) {
}
