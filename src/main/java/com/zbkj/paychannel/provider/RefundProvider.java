package com.zbkj.paychannel.provider;

import com.zbkj.paychannel.model.ChannelRefundCommand;
import com.zbkj.paychannel.model.RefundResult;

/**
 * 退款。
 *
 * <p>业务性失败（信用审查未通过、超期不可退等）用 {@code RefundResult.success=false + failReason}
 * 表达；仅在通信/系统异常时抛异常。两种情况编排层都会把退款单置为 FAIL 并保留记录。</p>
 */
public interface RefundProvider extends ChannelProvider {

    RefundResult refund(ChannelRefundCommand command);
}
