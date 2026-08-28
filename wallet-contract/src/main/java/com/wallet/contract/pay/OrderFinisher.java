package com.wallet.contract.pay;

/**
 * 结单器契约。由 {@code wallet-pay} 的 {@code OrderFinisherImpl} 实现：
 * 全部分段 SUCCESS 时把主单 CAS 推进 SUCCESS 并发布支付成功事件。
 *
 * <p>纯资产完成、渠道回调监听、补单三条路径共用，保证 markPaid 与事件发布只有这一处。
 * <b>必须在持单锁内调用。</b></p>
 */
public interface OrderFinisher {

    /**
     * 全部分段 SUCCESS 才推进主单（可退金额 = 总额 - 券面额）；
     * CAS 影响行数=1 时发布支付成功事件（对同一订单至多一次）。
     */
    void finishIfAllSuccess(String orderNo);
}
