package com.wallet.contract.pay.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 支付单详情（主单视图 + 分段视图）。
 *
 * @param order 主单视图
 * @param parts 全部分段视图
 */
@Schema(description = "支付单详情")
public record OrderDetail(
    @Schema(description = "支付主单") PayOrderView order,
    @Schema(description = "全部支付分段") List<PayPartView> parts) {
}
