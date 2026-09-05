package com.wallet.security.core;

/**
 * 支付安全跨端协议字面常量：协议版本与 App 渠道标识。
 *
 * <p>本类只承载不属于任何枚举、但作为协议字面值需要集中管理的硬编码字符串。
 * 跨端字面取值（包括各枚举的 {@code name()}）随票据、数据库记录和客户端协议
 * 持久化，属于协议本身而非配置，任何变更都等同协议升级。</p>
 *
 * <p>枚举类（{@code com.wallet.security.enums}）是状态/事件取值的权威定义；
 * 本类是协议版本与渠道标识等补充常量的承载点。</p>
 */
public final class PaySecurityConstants {

    private PaySecurityConstants() {
    }

    /** 票据协议版本（开放格式字符串，非枚举）。 */
    public static final String POLICY_VERSION = "v1";

    /** 客户端渠道：原生 App（生物注册票据只对该渠道顺带签发）。 */
    public static final String CLIENT_PLATFORM_APP = "app";
}