package com.wallet.app.limit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wallet.common.error.CommonError;
import com.wallet.common.result.ApiResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RedissonClient;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;

/**
 * 多维分布式限流：GLOBAL 应用兜底 → API 接口 → IP → USER，逐维检查。
 *
 * <ul>
 *   <li>规则来源：配置文件全局默认（GLOBAL/IP/USER）+ 接口 {@link RateLimit} 注解（同维度覆盖，API 仅注解）；</li>
 *   <li>算法：固定窗口计数（Redis 原子自增，1 秒一窗，key 带 2 秒 TTL 自动清理，
 *       高基数维度如 IP/USER 不会造成 key 堆积），多实例共享计数；</li>
 *   <li>超限返回 429 + 统一返回体（RATE_LIMITED，message 带命中维度）；</li>
 *   <li>Redis 不可用时放行——限流是保护手段，不能反过来把服务打死。</li>
 * </ul>
 */
@Slf4j
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RedissonClient redisson;
    private final ObjectMapper objectMapper;
    private final int qpsGlobal;
    private final int qpsPerIp;
    private final int qpsPerUser;

    public RateLimitInterceptor(RedissonClient redisson, ObjectMapper objectMapper,
        int qpsGlobal, int qpsPerIp, int qpsPerUser) {
        this.redisson = redisson;
        this.objectMapper = objectMapper;
        this.qpsGlobal = qpsGlobal;
        this.qpsPerIp = qpsPerIp;
        this.qpsPerUser = qpsPerUser;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
        throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        Map<LimitDim, Integer> rules = new EnumMap<>(LimitDim.class);
        if (qpsGlobal > 0) {
            rules.put(LimitDim.GLOBAL, qpsGlobal);
        }
        if (qpsPerIp > 0) {
            rules.put(LimitDim.IP, qpsPerIp);
        }
        if (qpsPerUser > 0) {
            rules.put(LimitDim.USER, qpsPerUser);
        }
        // 接口注解：同维度覆盖全局默认（API 维度只有注解能配）
        for (RateLimit annotation : handlerMethod.getMethod().getAnnotationsByType(RateLimit.class)) {
            rules.put(annotation.dim(), annotation.permits());
        }

        long windowSecond = System.currentTimeMillis() / 1000;
        try {
            for (Map.Entry<LimitDim, Integer> rule : rules.entrySet()) {
                String dimValue = dimValue(rule.getKey(), request, handlerMethod);
                if (dimValue == null) {
                    continue; // 该维度取不到标识（如无 X-Uid），跳过
                }
                String key = "wallet:rate:" + rule.getKey() + ':' + dimValue + ':' + windowSecond;
                RAtomicLong counter = redisson.getAtomicLong(key);
                long count = counter.incrementAndGet();
                if (count == 1) {
                    counter.expire(Duration.ofSeconds(2)); // 窗口过期即清，key 不堆积
                }
                if (count > rule.getValue()) {
                    reject(response, rule.getKey());
                    return false;
                }
            }
        } catch (Exception e) {
            log.warn("限流计数异常，本次放行, uri={}, err={}", request.getRequestURI(), e.getMessage());
        }
        return true;
    }

    private String dimValue(LimitDim dim, HttpServletRequest request, HandlerMethod handlerMethod) {
        return switch (dim) {
            case GLOBAL -> "all";
            case API -> handlerMethod.getBeanType().getSimpleName() + '.' + handlerMethod.getMethod().getName();
            case IP -> clientIp(request);
            case USER -> blankToNull(request.getHeader("X-Uid"));
        };
    }

    private void reject(HttpServletResponse response, LimitDim dim) throws Exception {
        response.setStatus(429);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(
            ApiResult.fail(CommonError.RATE_LIMITED.code(),
                CommonError.RATE_LIMITED.message() + "（" + dim + " 限流）")));
    }

    /** 客户端 IP：优先代理头（网关/LB 后面部署时），否则远端地址 */
    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            return comma > 0 ? forwarded.substring(0, comma).trim() : forwarded.trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        return realIp != null && !realIp.isBlank() ? realIp : request.getRemoteAddr();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
