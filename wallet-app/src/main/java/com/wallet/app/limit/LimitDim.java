package com.wallet.app.limit;

/**
 * 限流维度。规则来源两层：配置文件（全局默认）+ 接口注解 {@link RateLimit}（同维度覆盖全局）。
 */
public enum LimitDim {

    /** 应用级兜底：整个服务每秒总请求数（仅配置文件 wallet.rate-limit.qps-global，注解不用） */
    GLOBAL,

    /** 接口级：单个接口每秒总请求数（仅注解，配置文件无法逐接口配） */
    API,

    /** IP 级：每客户端 IP 每秒请求数（配置默认 qps-per-ip，注解可按接口收紧） */
    IP,

    /** 用户级：每用户（X-Uid）每秒请求数（配置默认 qps-per-user，注解可按接口收紧；无用户头时跳过） */
    USER
}
