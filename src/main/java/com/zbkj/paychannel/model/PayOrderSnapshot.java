package com.zbkj.paychannel.model;

import com.zbkj.paychannel.enums.PayStateEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 交易单快照（宿主持久化模型在 SDK 侧的只读视图）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayOrderSnapshot {

    private String orderNo;

    private String outTradeNo;

    private String channelCode;

    private PayStateEnum state;

    /** 实际请求渠道的支付金额（含手续费） */
    private BigDecimal payAmount;

    private String currency;

    /** 剩余可退金额 */
    private BigDecimal refundableAmount;

    /** 渠道侧交易号，可为 null */
    private String thirdOutTradeNo;
}
