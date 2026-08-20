package com.zbkj.paychannel.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 渠道查询结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryResult {

    /** 渠道侧是否已支付成功 */
    private boolean paid;

    /** 渠道侧交易号，可为 null */
    private String thirdOutTradeNo;
}
