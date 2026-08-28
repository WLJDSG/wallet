package com.wallet.contract.channel.model;

import com.wallet.common.enums.ActionType;

/**
 * 渠道调用日志（编排层在每次渠道调用前后组装，交给 CallLogWriter 落地）。
 *
 * <p>request/response 保留原始对象，由日志实现方决定怎么序列化——
 * 内核因此不依赖任何 JSON 库。</p>
 *
 * @param channelCode 渠道编码
 * @param action      动作
 * @param orderNo     业务订单号
 * @param outTradeNo  交易号
 * @param request     请求参数对象（可能为 null）
 * @param response    响应结果对象（异常时为 null）
 * @param error       异常摘要（无异常时为 null）
 * @param costMs      渠道调用耗时（毫秒）
 */
public record CallLog(String channelCode, ActionType action, String orderNo, String outTradeNo, Object request,
    Object response, String error, long costMs) {
}
