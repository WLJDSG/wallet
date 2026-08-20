package com.zbkj.paychannel.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 退款指令（编排层入参）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundCommand {

    private String channelCode;

    private String orderNo;

    /** 原支付交易号 */
    private String outTradeNo;

    /** 宿主生成的退款单号 */
    private String refundOrderNo;

    /** 申请退款金额（未含手续费加成，编排层经 FeePolicy 处理并按可退金额钳制） */
    private BigDecimal refundAmount;

    /**
     * 是否为外部已退款的补录（如线下已退、渠道后台手工退）：
     * true 时不调用渠道，仅登记退款单并推进状态。
     */
    @Builder.Default
    private boolean outRefund = false;

    /** 用户标识 */
    private Integer userId;

    /** 渠道扩展参数，SDK 不解析 */
    private Map<String, Object> extras;
}
