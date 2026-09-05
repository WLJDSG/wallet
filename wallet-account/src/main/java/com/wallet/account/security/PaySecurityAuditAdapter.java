package com.wallet.account.security;

import com.wallet.account.entity.PaySecurityAuditEntity;
import com.wallet.account.mapper.PaySecurityAuditMapper;
import com.wallet.security.spi.AuditRecorder;
import com.wallet.security.spi.model.PaySecurityAuditEvent;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

/** 安全事件同步落库；内核会隔离审计失败，不改变支付结果。 */
@Component
@AllArgsConstructor
public class PaySecurityAuditAdapter implements AuditRecorder {

    private final PaySecurityAuditMapper mapper;

    @Override
    public void record(PaySecurityAuditEvent event) {
        PaySecurityAuditEntity e = new PaySecurityAuditEntity();
        e.setEventType(event.getEventType().name()); e.setUid(event.getUid()); e.setOrderNo(event.getOrderNo());
        e.setCredentialId(event.getCredentialId()); e.setClientType(event.getClientType()); e.setAppVersion(event.getAppVersion());
        e.setResult(event.getResult().name()); e.setReasonCode(event.getReasonCode()); e.setAmount(event.getAmount());
        e.setCurrency(event.getCurrency()); e.setIp(event.getIp()); e.setUserAgentDigest(event.getUserAgentDigest());
        e.setOccurredAt(event.getOccurredAt());
        mapper.insert(e);
    }
}
