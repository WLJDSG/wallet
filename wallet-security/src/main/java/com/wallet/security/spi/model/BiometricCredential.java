package com.wallet.security.spi.model;

import com.wallet.security.enums.AttestationStatusEnum;
import com.wallet.security.enums.BiometricPlatformEnum;
import com.wallet.security.enums.CredentialDisabledReasonEnum;
import com.wallet.security.enums.CredentialStatusEnum;
import com.wallet.security.enums.SignAlgorithmEnum;
import lombok.Data;

import java.util.Date;

/**
 * 用户设备公钥对应的生物支付凭证。
 *
 * <p>服务端只保存公钥，私钥必须始终留在 Android Keystore 或 iOS Secure Enclave。
 * 凭证可用性由 status、passwordVersion 和 securityVersion 共同决定，不能只判断状态字段。</p>
 *
 * <p>所有限定取值字段在序列化时落库等价于对应枚举的 {@code name()}，
 * 历史脏数据缺值时反序列化为 null 而非抛错，避免审计/凭证查询中断。</p>
 */
@Data
public class BiometricCredential {

    /** 持久化自增主键，由仓储实现回填，仅用于服务端关联。 */
    private Integer id;

    /** 对客户端公开的生物支付凭证ID。 */
    private String credentialId;

    /** 创建凭证的一次性注册会话ID，需数据库唯一约束保证并发注册幂等。 */
    private String registrationId;

    /** 用户ID。 */
    private Long uid;

    /** 凭证注册平台，取值见 {@link BiometricPlatformEnum}。 */
    private BiometricPlatformEnum platform;

    /** 注册时支付密码版本。 */
    private Integer passwordVersion;

    /** 注册时支付安全版本。 */
    private Integer securityVersion;

    /** Base64编码的X.509 P-256公钥。 */
    private String publicKey;

    /** 签名算法，取值见 {@link SignAlgorithmEnum}。 */
    private SignAlgorithmEnum algorithm;

    /** 硬件密钥证明状态，取值见 {@link AttestationStatusEnum}。 */
    private AttestationStatusEnum keyAttestationStatus;

    /** App完整性证明状态，取值见 {@link AttestationStatusEnum}。 */
    private AttestationStatusEnum appIntegrityStatus;

    /** 凭证状态，取值见 {@link CredentialStatusEnum}。 */
    private CredentialStatusEnum status;

    /** 停用原因，取值见 {@link CredentialDisabledReasonEnum}。 */
    private CredentialDisabledReasonEnum disabledReason;

    /** 注册时间。 */
    private Date registeredAt;

    /** 最近成功使用时间。 */
    private Date lastUsedAt;

    /** 停用时间。 */
    private Date disabledAt;

}
