package com.wallet.security.spi.model;

import com.wallet.security.enums.AuditResultEnum;
import com.wallet.security.enums.ProtocolValue;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 支付密码、生物凭证和支付授权关键事件审计模型。
 *
 * <p>审计用于风控追溯，不参与交易成功判定。User-Agent 仅携带摘要，
 * 支付安全内核 保证不写入支付密码、短信验证码、原始授权票据或私钥等敏感信息。</p>
 */
@Data
public class PaySecurityAuditEvent {

    /**
     * 安全事件类型，取值集合为 {@link ProtocolValue} 的并集实现：
     * {@code AuditEventEnum} 与 {@code PasswordAuditEvent}。序列化时落库等价于各自枚举的 {@code name()}。
     */
    private ProtocolValue eventType;

    /** 用户ID。 */
    private Long uid;

    /** 关联订单号。 */
    private String orderNo;

    /** 关联生物支付凭证ID。 */
    private String credentialId;

    /** 客户端渠道（app/wx/h5/web 等开放扩展值）。 */
    private String clientType;

    /** App版本。 */
    private String appVersion;

    /**
     * 审计结果。序列化时落库等价于 {@link AuditResultEnum#name()}。
     */
    private AuditResultEnum result;

    /** 失败原因编码或授权方式编码，开放文本。 */
    private String reasonCode;

    /** 关联支付金额。 */
    private BigDecimal amount;

    /** 关联币种。 */
    private String currency;

    /** 请求IP。 */
    private String ip;

    /** User-Agent的SHA-256摘要。 */
    private String userAgentDigest;

    /** 事件发生时间。 */
    private Date occurredAt;

}
