package com.zbkj.paychannel.provider;

import com.zbkj.paychannel.model.QueryCommand;
import com.zbkj.paychannel.model.QueryResult;

/**
 * 主动查询支付结果。
 *
 * <p>实现本接口的渠道才参与：发起支付前对上一笔交易的查证、回调后的二次查证（reQueryRequired）、
 * 轮询兜底任务。未实现的渠道在这些路径上会得到 PAYMENT_ACTION_UNSUPPORTED 明确异常。</p>
 */
public interface QueryProvider extends ChannelProvider {

    QueryResult query(QueryCommand command);
}
