package com.wallet.channel.action;

import com.wallet.contract.channel.model.CancelRequest;

/**
 * 关闭/取消未支付交易。
 */
public interface CancelAction extends Channel {

    /**
     * @return 渠道是否关闭成功（交易在渠道侧本就不存在也应返回 true）
     */
    boolean cancel(CancelRequest request);
}
