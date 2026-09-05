package com.wallet.security.spi;

import com.wallet.security.enums.CredentialDisabledReasonEnum;
import com.wallet.security.enums.CredentialStatusEnum;
import com.wallet.security.spi.model.BiometricCredential;

import java.util.Date;
import java.util.List;

/**
 * 生物支付凭证持久化端口。
 *
 * <p>所有查询都显式带 uid 所有权条件，禁止用 credentialId 跨用户读取或撤销凭证。
 * registrationId 需建数据库唯一索引以保证并发注册幂等。</p>
 */
public interface BiometricCredentialRepository {

    /**
     * 查询用户的全部生物支付凭证。
     *
     * @param uid 用户ID
     * @return 凭证列表，可为空列表
     */
    List<BiometricCredential> findByUid(Long uid);

    /**
     * 查询用户拥有的指定凭证。
     *
     * @param uid 用户ID
     * @param credentialId 凭证ID
     * @return 凭证，不存在时返回 null
     */
    BiometricCredential findByUidAndCredentialId(Long uid, String credentialId);

    /**
     * 查询注册会话已创建的凭证，用于注册幂等返回。
     *
     * @param uid 用户ID
     * @param registrationId 注册会话ID
     * @return 凭证，不存在时返回 null
     */
    BiometricCredential findByUidAndRegistrationId(Long uid, String registrationId);

    /**
     * 新增凭证。实现必须在返回前将自增主键回填到 {@code credential.id}。
     *
     * @param credential 生物支付凭证
     */
    void insert(BiometricCredential credential);

    /**
     * 仅更新凭证的最近使用时间（部分更新）。
     *
     * <p>实现契约：只写 lastUsedAt 一个字段。禁止全字段回写：验签线程持有的
     * 旧快照会把并发撤销/停用后的凭证状态覆盖回 ENABLED（已失效凭证复活）。</p>
     *
     * @param uid 用户ID
     * @param credentialId 凭证ID
     * @param lastUsedAt 最近使用时间
     */
    void updateLastUsedAt(Long uid, String credentialId, Date lastUsedAt);

    /**
     * 撤销用户拥有的指定凭证（条件部分更新）。
     *
     * <p>实现契约：仅更新当前状态不为 revokedStatus 的记录，保留已撤销凭证的
     * 原始原因与时间；只写 status、disabledReason、disabledAt 三个字段。</p>
     *
     * @param uid 用户ID
     * @param credentialId 凭证ID
     * @param revokedStatus 目标撤销状态
     * @param reason 撤销原因
     * @param disabledAt 撤销时间
     * @return 影响行数；0 表示凭证不存在或已处于撤销状态
     */
    int revokeByUidAndCredentialId(Long uid, String credentialId, CredentialStatusEnum revokedStatus,
        CredentialDisabledReasonEnum reason, Date disabledAt);

    /**
     * 停用用户的全部有效凭证；仅更新当前仍为 enabledStatus 的记录，
     * 保留已撤销凭证的原始原因和时间。
     *
     * @param uid 用户ID
     * @param enabledStatus 当前有效状态
     * @param disabledStatus 目标停用状态
     * @param reason 停用原因
     * @param disabledAt 停用时间
     * @return 影响行数
     */
    int disableAllEnabledByUid(Long uid, CredentialStatusEnum enabledStatus, CredentialStatusEnum disabledStatus,
        CredentialDisabledReasonEnum reason, Date disabledAt);
}
