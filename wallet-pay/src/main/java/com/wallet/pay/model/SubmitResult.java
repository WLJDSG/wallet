package com.wallet.pay.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 提交支付结果。
 *
 * @param state          主单状态：SUCCESS 当场完成 / PAYING 已发起等回调
 * @param channelPayload 渠道支付参数（三方向前端展示），纯资产支付为 null
 * @param message        提示
 */
@Schema(description = "提交支付结果")
public record SubmitResult(
    @Schema(description = "主单状态：SUCCESS 当场完成 / PAYING 已发起等回调") String state,
    @Schema(description = "渠道支付参数（前端拉起支付用），纯资产支付为 null") Object channelPayload,
    @Schema(description = "提示") String message) {
}
