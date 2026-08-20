package com.zbkj.paychannel.provider;

import com.zbkj.paychannel.model.CancelCommand;

/**
 * 关闭/取消未支付交易。
 */
public interface CancelProvider extends ChannelProvider {

    /**
     * @return 渠道是否关闭成功（交易在渠道侧本就不存在也应返回 true）
     */
    boolean cancel(CancelCommand command);
}
