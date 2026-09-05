package com.wallet.security.testutil;

import com.wallet.security.enums.ProtocolValue;
import com.wallet.security.spi.AuditRecorder;
import com.wallet.security.spi.model.PaySecurityAuditEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * 记录审计事件的测试替身。
 *
 * <p>对外只暴露 {@link ProtocolValue#name()} 字面取值列表，便于断言
 * 业务事件 / 密码事件落库的字符串值；底层完整事件对象仍保留供调试。</p>
 */
public class RecordingAuditRecorder implements AuditRecorder {

    public final List<PaySecurityAuditEvent> events = new ArrayList<>();

    @Override
    public synchronized void record(PaySecurityAuditEvent event) {
        events.add(event);
    }

    public synchronized List<String> eventTypes() {
        List<String> types = new ArrayList<>();
        for (PaySecurityAuditEvent event : events) {
            ProtocolValue eventType = event.getEventType();
            types.add(eventType == null ? null : eventType.name());
        }
        return types;
    }
}
