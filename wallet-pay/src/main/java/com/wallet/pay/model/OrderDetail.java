package com.wallet.pay.model;

import com.wallet.pay.entity.PayOrder;
import com.wallet.pay.entity.PayPart;

import java.util.List;

/**
 * 支付单详情（主单 + 全部分段）。
 *
 * @param order 主单
 * @param parts 分段
 */
public record OrderDetail(PayOrder order, List<PayPart> parts) {
}
