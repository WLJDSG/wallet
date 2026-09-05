package com.wallet.account.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/** 设备生物支付公钥凭证。私钥始终只保存在设备安全硬件中。 */
@Data
@TableName("pay_biometric_credential")
public class BiometricCredentialEntity {

    @TableId(type = IdType.AUTO)
    private Integer id;
    private String credentialId;
    private String registrationId;
    private Long uid;
    private String platform;
    private Integer passwordVersion;
    private Integer securityVersion;
    private String publicKey;
    private String algorithm;
    private String keyAttestationStatus;
    private String appIntegrityStatus;
    private String status;
    private String disabledReason;
    private Date registeredAt;
    private Date lastUsedAt;
    private Date disabledAt;
}
