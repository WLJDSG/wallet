package com.wallet.security.enums;

/**
 * 支付安全跨端协议取值的联合标记。
 *
 * <p>用于表达一个取值同时来自多个枚举的场景（如审计表 event_type 既涵盖
 * {@link AuditEventEnum} 也涵盖 {@link PasswordAuditEvent} 中的密码生命周期事件）。
 * 标记为同一接口后，调用方既能编译期类型安全，又能在语义允许时再细化到具体枚举。</p>
 *
 * <p>所有 protocol 取值（包括 {@code AuditEventEnum} 和 {@code PasswordAuditEvent}）
 * 必须实现此接口；支付安全内核 入参凡是可接受这些取值的位置都应优先使用本接口类型，MyBatis
 * 写库时统一调用 {@link #name()}（即 {@code Enum#name()}）落 VARCHAR。</p>
 */
public interface ProtocolValue {

    /**
     * 跨端协议字面取值，与落库的字符串值完全相同。
     *
     * @return 数据库持久化字面取值
     */
    String name();
}
