package com.zbkj.paychannel.spi;

import com.zbkj.paychannel.model.PayLogRecord;

/**
 * 支付日志出口 SPI（宿主实现，可同步落库或异步投递）。
 *
 * <p>编排层已对 record 调用做 try/catch 兜底，实现内抛异常不会影响主流程；
 * 建议宿主用独立事务或消息队列落库（对应 crmeb 宿主的 PayLogAspect REQUIRES_NEW 语义）。</p>
 */
public interface PayLogSink {

    void record(PayLogRecord record);
}
