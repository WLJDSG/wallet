package com.zbkj.paychannel.model;

import com.zbkj.paychannel.enums.RefundStateEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 退款单快照。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundOrderSnapshot {

    private String refundOrderNo;

    /** 原支付交易号 */
    private String outTradeNo;

    private RefundStateEnum state;

    private BigDecimal refundAmount;

    private String currency;
}
