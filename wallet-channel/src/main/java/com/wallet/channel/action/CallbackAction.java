package com.wallet.channel.action;

import com.wallet.channel.model.CallbackRequest;
import com.wallet.channel.model.CallbackResult;

/**
 * 异步回调解析与验签。
 *
 * <p>契约：</p>
 * <ul>
 *   <li>必须在本方法内完成验签，验签失败抛 CALLBACK_VERIFY_FAILED——绝不允许未验签就返回 paid=true；</li>
 *   <li>验签必须基于 {@code request.body()} 原始报文；</li>
 *   <li>{@code ackBody} 无论支付成功与否都要按渠道协议填写，编排层在幂等跳过时也会原样应答。</li>
 * </ul>
 */
public interface CallbackAction extends Channel {

    CallbackResult onCallback(CallbackRequest request);
}
