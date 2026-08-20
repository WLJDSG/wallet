package com.zbkj.paychannel.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 渠道回调解析结果（Provider 产出，验签失败应抛 CALLBACK_VERIFY_FAILED 而不是返回本对象）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CallbackResult {

    /** 回调声明的支付结果 */
    private boolean paid;

    /** 渠道侧交易号 */
    private String thirdOutTradeNo;

    /**
     * 是否需要以主动查询结果为准（不信任回调报文的渠道设 true；
     * 该渠道必须同时实现 QueryProvider，注册表在启动期校验）。
     */
    @Builder.Default
    private boolean reQueryRequired = false;

    /** 应答渠道的报文体（无论处理是否幂等跳过都会原样返回给渠道） */
    private String ackBody;
}
