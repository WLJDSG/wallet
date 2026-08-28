package com.wallet.contract.channel.spi;

import com.wallet.contract.channel.model.CallLog;

/**
 * 渠道调用日志出口（调用方实现，可同步落库或异步投递）。
 *
 * <p>编排层已对 write 调用做 try/catch 兜底，实现内抛异常不会影响主流程；
 * 日志的 request/response 是原始对象，序列化方式由实现方决定（内核不依赖 JSON 库）。</p>
 */
public interface CallLogWriter {

    void write(CallLog log);
}
