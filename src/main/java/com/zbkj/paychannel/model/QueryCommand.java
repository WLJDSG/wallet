package com.zbkj.paychannel.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 主动查询支付结果指令。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryCommand {

    private String channelCode;

    private String orderNo;

    private String outTradeNo;

    /** 渠道侧交易号（部分渠道查询必填），可为 null */
    private String thirdOutTradeNo;
}
