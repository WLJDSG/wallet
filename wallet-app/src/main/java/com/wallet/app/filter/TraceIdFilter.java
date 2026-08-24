package com.wallet.app.filter;

import com.wallet.common.trace.TraceIds;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * TraceId 链路追踪入口：每个 HTTP 请求分配 traceId（优先透传上游 X-Trace-Id），
 * 写 MDC（日志 %X{traceId} 输出）并回写响应头；ApiResult 自动携带。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
        FilterChain filterChain) throws ServletException, IOException {
        String traceId = request.getHeader(TraceIds.HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = TraceIds.newTraceId();
        }
        TraceIds.put(traceId);
        response.setHeader(TraceIds.HEADER, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            TraceIds.clear();
        }
    }
}
