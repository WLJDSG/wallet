package com.zbkj.paychannel.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 发起支付指令。
 *
 * <p>金额语义：{@code payAmount} 为本次应付金额（渠道手续费由 {@code FeePolicy} 在编排层统一加成），
 * 精度按 {@code currency} 的最小货币单位由宿主 FeePolicy 决定。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayCommand {

    /** 渠道编码（宿主自定义，如 "JKOPAY"、"ANTOM"），需与 Provider 的 channelCode 一致 */
    private String channelCode;

    /** 业务订单号 */
    private String orderNo;

    /** 业务订单类型（宿主自定义语义，透传给渠道 Provider 拼订单描述等） */
    private String orderType;

    /** 应付金额（未含渠道手续费） */
    private BigDecimal payAmount;

    /** 币种（ISO 4217，如 TWD/JPY/CNY） */
    private String currency;

    /** 同一订单上一次发起支付的交易号；非空时编排器会先查证并关闭上一笔未支付交易 */
    private String lastOutTradeNo;

    /** 用户标识（宿主侧登录态，SDK 不解析） */
    private Integer userId;

    /** 客户端 IP（部分渠道风控必填） */
    private String clientIp;

    /** 渠道子类型/端型等扩展参数（如微信 isChannel、payChannel），SDK 不解析，原样透传给 Provider */
    private Map<String, Object> extras;
}
