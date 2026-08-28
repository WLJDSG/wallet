package com.wallet.contract.channel.action;

import com.wallet.contract.channel.model.ConfirmRequest;
import com.wallet.contract.channel.model.ConfirmResult;

/**
 * 二段式扣款确认（如 PayPal execute payment）。
 *
 * <p>编排层已做状态幂等：交易单非可流转状态时不会调用本方法，
 * 渠道实现无须自行防重，但仍应保证渠道侧调用幂等（渠道通常拒绝二次 capture）。</p>
 */
public interface ConfirmAction extends Channel {

    ConfirmResult confirm(ConfirmRequest request);
}
