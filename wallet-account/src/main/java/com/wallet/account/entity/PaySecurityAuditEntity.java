package com.wallet.account.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/** 支付密码、短信、生物认证及授权票据的安全审计记录。 */
@Data
@TableName("pay_security_audit")
public class PaySecurityAuditEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String eventType;
    private Long uid;
    private String orderNo;
    private String credentialId;
    private String clientType;
    private String appVersion;
    private String result;
    private String reasonCode;
    private BigDecimal amount;
    private String currency;
    private String ip;
    private String userAgentDigest;
    private Date occurredAt;
}
