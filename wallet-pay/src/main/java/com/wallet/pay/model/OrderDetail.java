package com.wallet.pay.model;

import io.swagger.v3.oas.annotations.media.Schema;
import com.wallet.pay.entity.PayOrder;
import com.wallet.pay.entity.PayPart;

import java.util.List;

/**
 * 支付单详情（主单 + 全部分段）。
 *
 * @param order 主单
 * @param parts 分段
 */
@Schema(description = "支付单详情")
public record OrderDetail(
    @Schema(description = "支付主单") PayOrder order,
    @Schema(description = "全部支付分段") List<PayPart> parts) {
}
