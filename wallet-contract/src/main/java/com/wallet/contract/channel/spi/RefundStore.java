package com.wallet.contract.channel.spi;

import com.wallet.contract.channel.enums.RefundState;
import com.wallet.contract.channel.model.RefundInfo;

/**
 * 退款单持久化接口（调用方实现）。
 */
public interface RefundStore {

    /**
     * 创建退款单（状态 INIT）。
     */
    RefundInfo create(String outTradeNo, String refundOrderNo, long amount, String currency);

    /**
     * 条件推进状态：仅当当前状态等于 from 时更新为 to（契约同 TradeStore.changeState）。
     */
    boolean changeState(String refundOrderNo, RefundState from, RefundState to);
}
