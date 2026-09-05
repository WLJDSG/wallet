package com.wallet.security.spi;

import com.wallet.security.spi.model.PaySecurityAuditEvent;

/**
 * 支付安全审计事件出口。
 *
 * <p>支付安全内核 在调用处统一 try/catch 并记录错误日志：审计写入失败不会改写已完成的
 * 授权校验结果。实现可同步落库、也可异步投递，但不得抛出异常影响主流程
 * （即使抛出也会被 支付安全内核 吞掉）。</p>
 */
public interface AuditRecorder {

    /**
     * 记录一条审计事件。
     *
     * @param event 审计事件
     */
    void record(PaySecurityAuditEvent event);
}
