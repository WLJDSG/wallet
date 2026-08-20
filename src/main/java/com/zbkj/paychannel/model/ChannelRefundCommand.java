package com.zbkj.paychannel.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 下发给渠道 Provider 的退款指令（编排层已完成金额钳制与全额判定）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChannelRefundCommand {

    private String channelCode;

    private String orderNo;

    private String outTradeNo;

    private String refundOrderNo;

    /** 最终退款金额（已钳制到可退金额） */
    private BigDecimal refundAmount;

    private String currency;

    /** 渠道侧交易号 */
    private String thirdOutTradeNo;

    /** 是否全额退款 */
    private boolean allRefund;

    private Integer userId;

    private Map<String, Object> extras;
}
