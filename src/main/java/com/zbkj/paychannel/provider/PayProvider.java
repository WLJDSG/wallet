package com.zbkj.paychannel.provider;

import com.zbkj.paychannel.model.PayCommand;
import com.zbkj.paychannel.model.PayOrderSnapshot;

/**
 * 发起支付（每个渠道必须实现）。
 */
public interface PayProvider extends ChannelProvider {

    /**
     * 调用渠道下单，返回前端拉起支付所需参数（jsConfig/二维码URL/跳转URL等，类型由渠道自行约定）。
     *
     * @param command 支付指令（金额已含手续费）
     * @param payOrder 已落库的交易单快照（提供 outTradeNo）
     * @throws com.zbkj.paychannel.exception.PayChannelException 渠道下单失败时抛出 CHANNEL_INVOKE_ERROR
     */
    Object pay(PayCommand command, PayOrderSnapshot payOrder);
}
