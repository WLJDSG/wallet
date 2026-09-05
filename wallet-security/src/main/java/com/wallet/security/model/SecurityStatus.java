package com.wallet.security.model;

import com.wallet.security.enums.AuthorizeMethodEnum;
import lombok.Data;

import java.util.Date;

/**
 * 余额支付安全状态，仅供前端选择交互流程。
 *
 * <p>对外字段使用枚举保证类型安全；序列化时落库等价于对应枚举的 {@code name()}，
 * 与客户端 App / H5 / 小程序历史协议字面取值兼容。</p>
 */
@Data
public class SecurityStatus {

    /** 是否已设置支付密码。 */
    private Boolean passwordSet;

    /** 余额支付当前是否锁定。 */
    private Boolean balancePayLocked;

    /** 锁定截止时间，未锁定时为空。 */
    private Date lockedUntil;

    /** 客户端指定的生物支付凭证是否可用。 */
    private Boolean biometricAvailable;

    /** 服务端推荐授权方式。 */
    private AuthorizeMethodEnum preferredMethod;

    /** 当前支付安全策略版本，开放文本。 */
    private String securityPolicyVersion;
}
