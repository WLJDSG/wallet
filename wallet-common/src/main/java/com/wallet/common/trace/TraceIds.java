package com.wallet.common.trace;

import org.slf4j.MDC;

import java.util.UUID;

/**
 * TraceId 链路追踪约定。
 *
 * <p>HTTP 请求由 wallet-app 的 TraceIdFilter 负责：优先取请求头 {@value #HEADER}（跨服务透传），
 * 没有则生成，写入 MDC 并回写响应头。异步/定时入口（XXL-Job、mock 回调线程）自行调
 * {@link #seed()} 播种、finally 里 {@link #clear()}。日志 pattern 通过 %X{traceId} 输出。</p>
 */
public final class TraceIds {

    /** MDC key，日志 pattern 用 %X{traceId} 引用 */
    public static final String TRACE_ID = "traceId";

    /** 跨服务透传的请求头/响应头 */
    public static final String HEADER = "X-Trace-Id";

    private TraceIds() {
    }

    /** 生成 32 位小写 hex traceId */
    public static String newTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /** 当前线程的 traceId，可能为 null（未播种的后台线程） */
    public static String current() {
        return MDC.get(TRACE_ID);
    }

    /** 写入指定 traceId（HTTP 入口用，透传上游） */
    public static void put(String traceId) {
        MDC.put(TRACE_ID, traceId);
    }

    /** 异步/定时任务入口播种新 traceId */
    public static String seed() {
        String traceId = newTraceId();
        MDC.put(TRACE_ID, traceId);
        return traceId;
    }

    public static void clear() {
        MDC.remove(TRACE_ID);
    }
}
