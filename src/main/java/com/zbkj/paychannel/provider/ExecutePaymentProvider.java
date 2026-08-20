package com.zbkj.paychannel.provider;

import com.zbkj.paychannel.model.ExecutePaymentCommand;
import com.zbkj.paychannel.model.ExecutePaymentResult;

/**
 * 二段式扣款（如 PayPal execute payment）。
 *
 * <p>编排层已做状态幂等：交易单非可流转状态时不会调用本方法，
 * Provider 无须自行防重，但仍应保证渠道侧调用幂等（渠道通常拒绝二次 capture）。</p>
 */
public interface ExecutePaymentProvider extends ChannelProvider {

    ExecutePaymentResult executePayment(ExecutePaymentCommand command);
}
