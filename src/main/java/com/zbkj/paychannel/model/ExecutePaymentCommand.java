package com.zbkj.paychannel.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 二段式扣款指令（如 PayPal execute payment：用户在渠道侧授权后，商户主动发起扣款确认）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutePaymentCommand {

    private String channelCode;

    private String orderNo;

    /** 交易号（PayPal 场景即 paymentId） */
    private String outTradeNo;

    /** 渠道授权凭据（如 PayPal payerId），SDK 不解析 */
    private Map<String, Object> extras;
}
