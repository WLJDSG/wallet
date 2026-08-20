package com.zbkj.paychannel.model;

import com.zbkj.paychannel.enums.PayActionEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 支付日志记录（编排层在每次渠道 Provider 调用前后组装，交给宿主 PayLogSink 落地）。
 *
 * <p>相比 crmeb-pay-service 的 ThreadLocal + AOP 方案，本 SDK 在编排层同步组装日志，
 * 不依赖切面存在、无 ThreadLocal 泄漏风险，渠道实现也无须感知日志。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayLogRecord {

    private String channelCode;

    private PayActionEnum action;

    private String orderNo;

    private String outTradeNo;

    /** 请求参数 JSON */
    private String requestJson;

    /** 响应结果 JSON（异常时为 null） */
    private String responseJson;

    /** 异常摘要（无异常时为 null） */
    private String errorMessage;

    /** 渠道调用耗时（毫秒） */
    private long costMillis;
}
