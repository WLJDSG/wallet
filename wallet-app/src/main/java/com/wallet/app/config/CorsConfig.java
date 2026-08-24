package com.wallet.app.config;

import com.wallet.common.trace.TraceIds;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 跨域配置：联调放开全部来源（allowedOriginPatterns 配合 allowCredentials 合法），
 * 生产按需收紧为白名单域名。响应头暴露 X-Trace-Id 供前端上报排障。
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

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
}
