package com.wallet.security.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 调用方客户端信息快照。
 *
 * <p>支付安全内核 内部不读取任何 ThreadLocal 或 HTTP 上下文，渠道、版本、IP、User-Agent
 * 一律由宿主在调用时显式传入；字段允许为 null（如非 HTTP 上下文）。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientInfo {

    /** 客户端渠道：app、wx、h5、web 等，宿主定义。 */
    private String platform;

    /** 原生系统标识：android 或 ios（生物注册场景使用）。 */
    private String system;

    /** 客户端十进制 build 版本号。 */
    private String appVersion;

    /** 请求来源 IP，仅用于审计。 */
    private String ip;

    /** 原始 User-Agent，支付安全内核 审计时只保存其 SHA-256 摘要。 */
    private String userAgent;
}
