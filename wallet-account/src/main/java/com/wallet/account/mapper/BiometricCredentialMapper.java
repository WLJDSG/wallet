package com.wallet.account.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wallet.account.entity.BiometricCredentialEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.Date;
import java.util.List;

/** 生物支付凭证数据访问。所有单凭证操作必须同时带用户所有权条件。 */
@Mapper
public interface BiometricCredentialMapper extends BaseMapper<BiometricCredentialEntity> {

    default List<BiometricCredentialEntity> findByUid(Long uid) {
        return selectList(new LambdaQueryWrapper<BiometricCredentialEntity>()
            .eq(BiometricCredentialEntity::getUid, uid));
    }

    default BiometricCredentialEntity findByCredential(Long uid, String credentialId) {
        return selectOne(new LambdaQueryWrapper<BiometricCredentialEntity>()
            .eq(BiometricCredentialEntity::getUid, uid)
            .eq(BiometricCredentialEntity::getCredentialId, credentialId));
    }

    default BiometricCredentialEntity findByRegistration(Long uid, String registrationId) {
        return selectOne(new LambdaQueryWrapper<BiometricCredentialEntity>()
            .eq(BiometricCredentialEntity::getUid, uid)
            .eq(BiometricCredentialEntity::getRegistrationId, registrationId));
    }

    default void updateLastUsedAt(Long uid, String credentialId, Date lastUsedAt) {
        update(new LambdaUpdateWrapper<BiometricCredentialEntity>()
            .eq(BiometricCredentialEntity::getUid, uid)
            .eq(BiometricCredentialEntity::getCredentialId, credentialId)
            .set(BiometricCredentialEntity::getLastUsedAt, lastUsedAt));
    }

    default int revoke(Long uid, String credentialId, String status, String reason, Date disabledAt) {
        return update(new LambdaUpdateWrapper<BiometricCredentialEntity>()
            .eq(BiometricCredentialEntity::getUid, uid)
            .eq(BiometricCredentialEntity::getCredentialId, credentialId)
            .ne(BiometricCredentialEntity::getStatus, status)
            .set(BiometricCredentialEntity::getStatus, status)
            .set(BiometricCredentialEntity::getDisabledReason, reason)
            .set(BiometricCredentialEntity::getDisabledAt, disabledAt));
    }

    default int disableAll(Long uid, String enabled, String disabled, String reason, Date disabledAt) {
        return update(new LambdaUpdateWrapper<BiometricCredentialEntity>()
            .eq(BiometricCredentialEntity::getUid, uid)
            .eq(BiometricCredentialEntity::getStatus, enabled)
            .set(BiometricCredentialEntity::getStatus, disabled)
            .set(BiometricCredentialEntity::getDisabledReason, reason)
            .set(BiometricCredentialEntity::getDisabledAt, disabledAt));
    }
}
