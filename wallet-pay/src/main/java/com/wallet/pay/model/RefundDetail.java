package com.wallet.pay.model;

import com.wallet.pay.entity.RefundOrder;
import com.wallet.pay.entity.RefundPart;

import java.util.List;

/**
 * 退款单详情（退款主单 + 退款分段）。
 *
 * @param refundOrder 退款主单
 * @param parts       退款分段
 */
public record RefundDetail(RefundOrder refundOrder, List<RefundPart> parts) {
}
