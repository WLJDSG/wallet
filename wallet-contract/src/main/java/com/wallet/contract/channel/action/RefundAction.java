package com.wallet.contract.channel.action;

import com.wallet.contract.channel.model.ChannelRefundRequest;
import com.wallet.contract.channel.model.RefundResult;

/**
 * 退款。
 *
 * <p>业务性失败（信用审查未通过、超期不可退等）用 {@code RefundResult.fail(原因)} 表达；
 * 仅在通信/系统异常时抛异常。两种情况编排层都会把退款单置为 FAIL 并保留记录。</p>
 */
public interface RefundAction extends Channel {

    RefundResult refund(ChannelRefundRequest request);
}
