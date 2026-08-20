package com.zbkj.paychannel.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 发起支付结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayResult {

    /** 业务订单号 */
    private String orderNo;

    /** 本次交易号（由宿主 PayOrderRepository 创建交易单时生成） */
    private String outTradeNo;

    /** 实际请求渠道的金额（已含手续费） */
    private BigDecimal payAmount;

    /** 币种 */
    private String currency;

    /**
     * 渠道返回的前端拉起支付所需参数（jsConfig/二维码/跳转URL等），
     * 具体类型由各渠道 Provider 约定，宿主 Controller 负责组装响应。
     */
    private Object channelPayload;

    /**
     * 该渠道是否支持主动查询（QUERY）。
     * 宿主只应对 queryable=true 的交易安排轮询兜底任务；
     * 对不支持查询的渠道（如二段式扣款的 PayPal）安排轮询只会得到明确异常。
     */
    private boolean queryable;
}
