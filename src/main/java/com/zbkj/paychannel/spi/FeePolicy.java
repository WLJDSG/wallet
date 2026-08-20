package com.zbkj.paychannel.spi;

import java.math.BigDecimal;

/**
 * 渠道手续费策略 SPI（宿主实现，可选——默认不加费）。
 *
 * <p>把 crmeb-pay-service 里散落在编排层的 setPayFee if/else 收敛为策略接口，
 * 费率来源（常量/配置中心）、取整规则（按币种最小单位）由宿主决定。</p>
 */
public interface FeePolicy {

    /** 默认策略：原价，不加手续费 */
    FeePolicy NO_FEE = (channelCode, amount, currency) -> amount;

    /**
     * 计算含手续费的实际支付金额。
     *
     * @param channelCode 渠道编码
     * @param amount      应付金额
     * @param currency    币种（实现方按币种小数位取整，勿硬编码 setScale(0)）
     * @return 含费金额
     */
    BigDecimal applyPayFee(String channelCode, BigDecimal amount, String currency);
}
