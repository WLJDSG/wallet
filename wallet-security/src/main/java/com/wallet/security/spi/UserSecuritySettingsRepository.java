package com.wallet.security.spi;

import com.wallet.security.spi.model.UserSecuritySettings;

/**
 * 用户支付安全设置（密码哈希与安全版本）持久化端口。
 *
 * <p>一个 uid 只能存在一条设置。</p>
 */
public interface UserSecuritySettingsRepository {

    /**
     * 按用户查询支付安全设置。
     *
     * @param uid 用户ID
     * @return 设置，不存在时返回 null
     */
    UserSecuritySettings findByUid(Long uid);

    /**
     * 新增设置。实现必须在返回前将自增主键回填到 {@code settings.id}。
     *
     * @param settings 用户支付安全设置
     */
    void insert(UserSecuritySettings settings);

    /**
     * 按安全版本条件更新设置（乐观锁）。
     *
     * <p>实现契约：以 uid 定位记录，仅当其当前 securityVersion 等于
     * expectedSecurityVersion 时才写入，且只允许写 passwordHash、passwordVersion、
     * securityVersion、passwordStatus、passwordSetAt、passwordUpdatedAt 六个业务字段。
     * 禁止无条件的全字段回写：并发的改密与全量撤销会互相覆盖版本递增，
     * 甚至用旧快照把新密码哈希写回旧值。</p>
     *
     * @param settings 携带新值的设置
     * @param expectedSecurityVersion 调用方读取设置时的 securityVersion 快照
     * @return 恰好更新一行返回 true；版本已漂移（并发修改）返回 false
     */
    boolean updateWithVersion(UserSecuritySettings settings, int expectedSecurityVersion);
}
