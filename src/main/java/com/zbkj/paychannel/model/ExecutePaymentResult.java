package com.zbkj.paychannel.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 二段式扣款结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutePaymentResult {

    /** 扣款是否成功 */
    private boolean success;

    /** 渠道侧交易号 */
    private String thirdOutTradeNo;

    /** 失败原因（success=false 时填写） */
    private String failReason;
}
