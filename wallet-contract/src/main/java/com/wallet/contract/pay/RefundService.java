package com.wallet.contract.pay;

import com.wallet.contract.pay.model.RefundCreateResult;
import com.wallet.contract.pay.model.RefundDetail;

/**
 * 退款编排契约。由 {@code wallet-pay} 的 {@code RefundServiceImpl} 实现，Web 层经此调用。
 */
public interface RefundService {

    /** 发起退款：先退三方后退资产；持同一把支付单锁与支付/回调互斥。 */
    RefundCreateResult create(Long userId, String orderNo, long amount, String reason);

    /** 查退款单详情（退款单 + 分摊分段）。 */
    RefundDetail detail(Long userId, String refundNo);
}
