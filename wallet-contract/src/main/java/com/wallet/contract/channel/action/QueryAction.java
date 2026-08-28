package com.wallet.contract.channel.action;

import com.wallet.contract.channel.model.QueryRequest;
import com.wallet.contract.channel.model.QueryResult;

/**
 * 主动查询支付结果。
 *
 * <p>实现本接口的渠道才参与：发起支付前对上一笔交易的查证、回调后的二次查证（reQueryRequired）、
 * 轮询兜底任务。未实现的渠道在这些路径上会得到 PAYMENT_ACTION_UNSUPPORTED 明确异常。</p>
 */
public interface QueryAction extends Channel {

    QueryResult query(QueryRequest request);
}
