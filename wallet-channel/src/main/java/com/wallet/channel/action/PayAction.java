package com.wallet.channel.action;

import com.wallet.channel.model.PayRequest;
import com.wallet.channel.model.TradeInfo;

/**
 * 发起支付（每个渠道必须实现）。
 */
public interface PayAction extends Channel {

    /**
     * 调用渠道下单，返回前端拉起支付所需参数（jsConfig/二维码URL/跳转URL等，类型由渠道自行约定）。
     *
     * @param request 支付请求（金额已含手续费）
     * @param trade   已落库的交易单快照（提供 outTradeNo）
     * @throws com.wallet.channel.error.ChannelException 渠道下单失败时抛出 CHANNEL_INVOKE_ERROR
     */
    Object pay(PayRequest request, TradeInfo trade);
}
