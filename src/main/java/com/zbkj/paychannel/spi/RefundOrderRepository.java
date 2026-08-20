package com.zbkj.paychannel.spi;

import com.zbkj.paychannel.enums.RefundStateEnum;
import com.zbkj.paychannel.model.RefundOrderSnapshot;

import java.math.BigDecimal;

/**
 * 退款单持久化 SPI（宿主实现）。
 */
public interface RefundOrderRepository {

    /**
     * 创建退款单（状态 INIT）。
     */
    RefundOrderSnapshot create(String outTradeNo, String refundOrderNo, BigDecimal refundAmount, String currency);

    /**
     * 条件推进状态：仅当当前状态等于 from 时更新为 to（契约同 PayOrderRepository.transitionState）。
     */
    boolean transitionState(String refundOrderNo, RefundStateEnum from, RefundStateEnum to);
}
