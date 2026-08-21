package com.wallet.channel.spi;

/**
 * 渠道手续费规则（调用方实现，可选——默认不加费）。
 *
 * <p>费率来源（常量/配置中心）与取整规则由实现方决定；金额单位分，整数运算无精度问题。</p>
 */
public interface FeeRule {

    /** 默认规则：原价，不加手续费 */
    FeeRule NO_FEE = (channelCode, amount, currency) -> amount;

    /**
     * 计算含手续费的实际支付金额。
     *
     * @param channelCode 渠道编码
     * @param amount      应付金额，单位分
     * @param currency    币种
     * @return 含费金额，单位分
     */
    long applyFee(String channelCode, long amount, String currency);
}
