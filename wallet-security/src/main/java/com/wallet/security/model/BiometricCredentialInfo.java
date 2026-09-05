package com.wallet.security.model;

import com.wallet.security.enums.BiometricPlatformEnum;
import com.wallet.security.enums.CredentialStatusEnum;
import lombok.Data;

import java.util.Date;

/**
 * 生物支付凭证对外信息。
 *
 * <p>对外字段使用枚举保证类型安全；序列化时落库等价于对应枚举的 {@code name()}，
 * 与客户端 App / H5 / 小程序历史协议字面取值兼容。</p>
 */
@Data
public class BiometricCredentialInfo {

    /** 生物支付凭证ID。 */
    private String credentialId;

    /** 凭证注册平台。 */
    private BiometricPlatformEnum platform;

    /** 服务端凭证状态。 */
    private CredentialStatusEnum status;

    /** 凭证状态和安全版本是否允许用于当前支付。 */
    private Boolean available;

    /** 最近成功使用时间。 */
    private Date lastUsedAt;
}
