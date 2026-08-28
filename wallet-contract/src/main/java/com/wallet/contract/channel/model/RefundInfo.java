package com.wallet.contract.channel.model;

import com.wallet.contract.channel.enums.RefundState;

/**
 * 退款单快照。
 *
 * @param refundOrderNo 退款单号
 * @param outTradeNo    原支付交易号
 * @param state         当前状态
 * @param amount        退款金额，单位分
 * @param currency      币种
 */
public record RefundInfo(String refundOrderNo, String outTradeNo, RefundState state, long amount,
    String currency) {
}
