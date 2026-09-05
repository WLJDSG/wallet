package com.wallet.security.enums;

/**
 * 审计事件类型联合工厂：负责把 {@link AuditEventEnum} 与 {@link PasswordAuditEvent}
 * 安全地收敛为统一的 {@link ProtocolValue}，作为审计入口与落库字段的类型。
 *
 * <p>在编译期穷举密码事件，BIOMETRIC_ENROLLMENT 等非审计用途一旦误用立即报错，
 * 杜绝把"票据用途"误当成"审计事件"落库。</p>
 */
public final class AuditEventType {

    private AuditEventType() {
    }

    /** 把 {@link AuditEventEnum} 适配为审计事件类型。 */
    public static ProtocolValue of(AuditEventEnum event) {
        return event;
    }

    /**
     * 把 {@link PasswordAuditEvent} 适配为审计事件类型。
     *
     * @param event 密码生命周期审计事件
     * @return 审计事件类型
     */
    public static ProtocolValue of(PasswordAuditEvent event) {
        return event;
    }
}
