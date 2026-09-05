package com.wallet.security.spi.model;

import com.wallet.security.enums.PayPasswordStatusEnum;
import lombok.Data;

import java.util.Date;

/**
 * 用户支付密码和全局支付安全版本设置。
 *
 * <p>passwordVersion 管理密码与凭证的绑定关系，securityVersion 是更高层的全局失效开关。
 * 密码修改、重置或全量撤销凭证时由 支付安全内核 按业务规则递增版本，使历史票据与公钥立即失效。</p>
 *
 * <p>所有限定取值字段在序列化时落库等价于对应枚举的 {@code name()}。</p>
 */
@Data
public class UserSecuritySettings {

    /** 持久化自增主键，由仓储实现回填。 */
    private Integer id;

    /** 用户ID。 */
    private Long uid;

    /** BCrypt支付密码慢哈希。 */
    private String passwordHash;

    /** 支付密码版本，设置、修改或重置时变化。 */
    private Integer passwordVersion;

    /** 全局支付安全版本，凭证撤销等安全状态变化时递增。 */
    private Integer securityVersion;

    /** 支付密码状态，取值见 {@link PayPasswordStatusEnum}。 */
    private PayPasswordStatusEnum passwordStatus;

    /** 首次设置支付密码时间。 */
    private Date passwordSetAt;

    /** 最近修改或重置支付密码时间。 */
    private Date passwordUpdatedAt;

}
