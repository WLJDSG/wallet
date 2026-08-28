package com.wallet.contract.pay.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 退款单详情（退款主单视图 + 退款分段视图）。
 *
 * @param refundOrder 退款主单视图
 * @param parts       退款分摊分段视图
 */
@Schema(description = "退款单详情")
public record RefundDetail(
    @Schema(description = "退款主单") RefundOrderView refundOrder,
    @Schema(description = "退款分摊分段") List<RefundPartView> parts) {
}
