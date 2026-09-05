package com.wallet.security.testutil;

import com.wallet.security.enums.CredentialDisabledReasonEnum;
import com.wallet.security.enums.CredentialStatusEnum;
import com.wallet.security.spi.BiometricCredentialRepository;
import com.wallet.security.spi.model.BiometricCredential;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 生物凭证仓储的内存实现。模拟数据库行为：查询返回副本、insert 回填自增 id、
 * 更新按 SPI 契约做条件部分更新，避免测试对象别名掩盖遗漏的写库调用。
 */
public class InMemoryCredentialRepository implements BiometricCredentialRepository {

    private final List<BiometricCredential> rows = new ArrayList<>();
    private final AtomicInteger idSeq = new AtomicInteger();

    @Override
    public synchronized List<BiometricCredential> findByUid(Long uid) {
        List<BiometricCredential> result = new ArrayList<>();
        for (BiometricCredential row : rows) {
            if (row.getUid().equals(uid)) {
                result.add(copy(row));
            }
        }
        return result;
    }

    @Override
    public synchronized BiometricCredential findByUidAndCredentialId(Long uid, String credentialId) {
        for (BiometricCredential row : rows) {
            if (row.getUid().equals(uid) && Objects.equals(row.getCredentialId(), credentialId)) {
                return copy(row);
            }
        }
        return null;
    }

    @Override
    public synchronized BiometricCredential findByUidAndRegistrationId(Long uid, String registrationId) {
        for (BiometricCredential row : rows) {
            if (row.getUid().equals(uid) && Objects.equals(row.getRegistrationId(), registrationId)) {
                return copy(row);
            }
        }
        return null;
    }

    @Override
    public synchronized void insert(BiometricCredential credential) {
        credential.setId(idSeq.incrementAndGet());
        rows.add(copy(credential));
    }

    @Override
    public synchronized void updateLastUsedAt(Long uid, String credentialId, Date lastUsedAt) {
        for (BiometricCredential row : rows) {
            if (row.getUid().equals(uid) && Objects.equals(row.getCredentialId(), credentialId)) {
                row.setLastUsedAt(lastUsedAt);
            }
        }
    }

    @Override
    public synchronized int revokeByUidAndCredentialId(Long uid, String credentialId,
        CredentialStatusEnum revokedStatus, CredentialDisabledReasonEnum reason, Date disabledAt) {
        int affected = 0;
        for (BiometricCredential row : rows) {
            if (row.getUid().equals(uid) && Objects.equals(row.getCredentialId(), credentialId)
                && row.getStatus() != revokedStatus) {
                row.setStatus(revokedStatus);
                row.setDisabledReason(reason);
                row.setDisabledAt(disabledAt);
                affected++;
            }
        }
        return affected;
    }

    @Override
    public synchronized int disableAllEnabledByUid(Long uid, CredentialStatusEnum enabledStatus,
        CredentialStatusEnum disabledStatus, CredentialDisabledReasonEnum reason, Date disabledAt) {
        int affected = 0;
        for (BiometricCredential row : rows) {
            if (row.getUid().equals(uid) && row.getStatus() == enabledStatus) {
                row.setStatus(disabledStatus);
                row.setDisabledReason(reason);
                row.setDisabledAt(disabledAt);
                affected++;
            }
        }
        return affected;
    }

    private BiometricCredential copy(BiometricCredential source) {
        BiometricCredential target = new BiometricCredential();
        target.setId(source.getId());
        target.setCredentialId(source.getCredentialId());
        target.setRegistrationId(source.getRegistrationId());
        target.setUid(source.getUid());
        target.setPlatform(source.getPlatform());
        target.setPasswordVersion(source.getPasswordVersion());
        target.setSecurityVersion(source.getSecurityVersion());
        target.setPublicKey(source.getPublicKey());
        target.setAlgorithm(source.getAlgorithm());
        target.setKeyAttestationStatus(source.getKeyAttestationStatus());
        target.setAppIntegrityStatus(source.getAppIntegrityStatus());
        target.setStatus(source.getStatus());
        target.setDisabledReason(source.getDisabledReason());
        target.setRegisteredAt(source.getRegisteredAt());
        target.setLastUsedAt(source.getLastUsedAt());
        target.setDisabledAt(source.getDisabledAt());
        return target;
    }
}
