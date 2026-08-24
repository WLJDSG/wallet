package com.wallet.app.limit;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口级限流注解（可重复标注多个维度），同维度覆盖配置文件的全局默认值。
 *
 * <pre>{@code
 * @RateLimit(dim = LimitDim.USER, permits = 3)   // 该接口每用户每秒 3 次（防爆破）
 * @RateLimit(dim = LimitDim.IP, permits = 10)    // 该接口每 IP 每秒 10 次
 * @RateLimit(dim = LimitDim.API, permits = 100)  // 该接口总 QPS 100
 * }</pre>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Repeatable(RateLimits.class)
public @interface RateLimit {

    /** 限流维度（GLOBAL 是应用兜底，只在配置文件配，不在注解用） */
    LimitDim dim();

    /** 每秒许可数 */
    int permits();
}
