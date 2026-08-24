package com.wallet.pay.model;

import io.swagger.v3.oas.annotations.media.Schema;
import com.wallet.pay.entity.RefundOrder;
import com.wallet.pay.entity.RefundPart;

import java.util.List;

/**
 * 退款单详情（退款主单 + 退款分段）。
 *
 * @param refundOrder 退款主单
 * @param parts       退款分段
 */
@Schema(description = "退款单详情")
public record RefundDetail(
    @Schema(description = "退款主单") RefundOrder refundOrder,
    @Schema(description = "退款分摊分段") List<RefundPart> parts) {
}
