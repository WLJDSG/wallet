package com.zbkj.paychannel.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 关闭/取消交易指令。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancelCommand {

    private String channelCode;

    private String orderNo;

    private String outTradeNo;

    /** 渠道侧交易号，可为 null */
    private String thirdOutTradeNo;
}
