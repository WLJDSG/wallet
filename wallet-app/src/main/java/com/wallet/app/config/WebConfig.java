package com.wallet.app.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wallet.app.limit.RateLimitInterceptor;
import com.wallet.common.trace.TraceIds;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 层统一装配（MVC 扩展点收口在这一个类）：跨域 + 限流拦截器。
 *
 * <ul>
 *   <li><b>跨域</b>：联调放开全部来源（allowedOriginPatterns 配合 allowCredentials 合法），
 *       生产按需收紧为白名单域名；响应头暴露 X-Trace-Id 供前端上报排障。</li>
 *   <li><b>限流</b>：多维分布式固定窗口（GLOBAL 应用兜底 / API 接口 / IP / USER），
 *       配置文件配全局默认、接口 @RateLimit 注解按维度覆盖，超阈值 429；
 *       `wallet.rate-limit.enabled=false` 可关闭（网关已有限流时）。</li>
 * </ul>
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;
    private final boolean rateLimitEnabled;
    private final int qpsGlobal;
    private final int qpsPerIp;
    private final int qpsPerUser;

    public WebConfig(RedissonClient redissonClient, ObjectMapper objectMapper,
        @Value("${wallet.rate-limit.enabled:true}") boolean rateLimitEnabled,
        @Value("${wallet.rate-limit.qps-global:1000}") int qpsGlobal,
        @Value("${wallet.rate-limit.qps-per-ip:50}") int qpsPerIp,
        @Value("${wallet.rate-limit.qps-per-user:20}") int qpsPerUser) {
        this.redissonClient = redissonClient;
        this.objectMapper = objectMapper;
        this.rateLimitEnabled = rateLimitEnabled;
        this.qpsGlobal = qpsGlobal;
        this.qpsPerIp = qpsPerIp;
        this.qpsPerUser = qpsPerUser;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
            .allowedOriginPatterns("*")
            .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
            .allowedHeaders("*")
            .exposedHeaders(TraceIds.HEADER)
            .allowCredentials(true)
            .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        if (rateLimitEnabled) {
            registry.addInterceptor(new RateLimitInterceptor(redissonClient, objectMapper,
                    qpsGlobal, qpsPerIp, qpsPerUser))
                .addPathPatterns("/api/**");
        }
    }
}
