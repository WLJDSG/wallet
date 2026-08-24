package com.wallet.common.result;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.wallet.common.trace.TraceIds;

import java.time.LocalDateTime;

/**
 * 统一接口返回体。traceId 自动取当前线程 MDC（TraceIdFilter 写入），排障时可凭它串日志。
 *
 * @param code      "0" 表示成功，其他为错误码
 * @param message   提示信息
 * @param data      业务数据，可为 null
 * @param traceId   本次请求链路 ID，可为 null（未经过 HTTP 入口的场景）
 * @param timestamp 响应时间
 */
public record ApiResult<T>(String code, String message, T data, String traceId, LocalDateTime timestamp) {

    public static final String OK_CODE = "0";

    public static <T> ApiResult<T> ok(T data) {
        return build(OK_CODE, "ok", data);
    }

    public static ApiResult<Void> ok() {
        return build(OK_CODE, "ok", null);
    }

    public static <T> ApiResult<T> fail(String code, String message) {
        return build(code, message, null);
    }

    private static <T> ApiResult<T> build(String code, String message, T data) {
        return new ApiResult<>(code, message, data, TraceIds.current(), LocalDateTime.now());
    }

    /** 派生字段：前端可直接判 success，不用比对 code */
    @JsonProperty("success")
    public boolean success() {
        return OK_CODE.equals(code);
    }
}
