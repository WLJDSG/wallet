package com.wallet.account.security;

import com.wallet.account.entity.BiometricCredentialEntity;
import com.wallet.account.mapper.BiometricCredentialMapper;
import com.wallet.security.enums.AttestationStatusEnum;
import com.wallet.security.enums.BiometricPlatformEnum;
import com.wallet.security.enums.CredentialDisabledReasonEnum;
import com.wallet.security.enums.CredentialStatusEnum;
import com.wallet.security.enums.SignAlgorithmEnum;
import com.wallet.security.spi.BiometricCredentialRepository;
import com.wallet.security.spi.model.BiometricCredential;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

/** 生物支付凭证仓储适配器。 */
@Component
@AllArgsConstructor
public class BiometricCredentialAdapter implements BiometricCredentialRepository {

    private final BiometricCredentialMapper mapper;

    @Override
    public List<BiometricCredential> findByUid(Long uid) {
        return mapper.findByUid(uid).stream().map(this::toModel).toList();
    }

    @Override
    public BiometricCredential findByUidAndCredentialId(Long uid, String credentialId) {
        return toModel(mapper.findByCredential(uid, credentialId));
    }

    @Override
    public BiometricCredential findByUidAndRegistrationId(Long uid, String registrationId) {
        return toModel(mapper.findByRegistration(uid, registrationId));
    }

    @Override
    public void insert(BiometricCredential credential) {
        BiometricCredentialEntity entity = toEntity(credential);
        mapper.insert(entity);
        credential.setId(entity.getId());
    }

    @Override
    public void updateLastUsedAt(Long uid, String credentialId, Date lastUsedAt) {
        mapper.updateLastUsedAt(uid, credentialId, lastUsedAt);
    }

    @Override
    public int revokeByUidAndCredentialId(Long uid, String credentialId, CredentialStatusEnum status,
        CredentialDisabledReasonEnum reason, Date disabledAt) {
        return mapper.revoke(uid, credentialId, status.name(), reason.name(), disabledAt);
    }

    @Override
    public int disableAllEnabledByUid(Long uid, CredentialStatusEnum enabled, CredentialStatusEnum disabled,
        CredentialDisabledReasonEnum reason, Date disabledAt) {
        return mapper.disableAll(uid, enabled.name(), disabled.name(), reason.name(), disabledAt);
    }

    private BiometricCredential toModel(BiometricCredentialEntity e) {
        if (e == null) return null;
        BiometricCredential m = new BiometricCredential();
        m.setId(e.getId()); m.setCredentialId(e.getCredentialId()); m.setRegistrationId(e.getRegistrationId());
        m.setUid(e.getUid()); m.setPlatform(BiometricPlatformEnum.valueOf(e.getPlatform()));
        m.setPasswordVersion(e.getPasswordVersion()); m.setSecurityVersion(e.getSecurityVersion());
        m.setPublicKey(e.getPublicKey()); m.setAlgorithm(SignAlgorithmEnum.valueOf(e.getAlgorithm()));
        m.setKeyAttestationStatus(AttestationStatusEnum.valueOf(e.getKeyAttestationStatus()));
        m.setAppIntegrityStatus(AttestationStatusEnum.valueOf(e.getAppIntegrityStatus()));
        m.setStatus(CredentialStatusEnum.valueOf(e.getStatus()));
        m.setDisabledReason(e.getDisabledReason() == null ? null : CredentialDisabledReasonEnum.valueOf(e.getDisabledReason()));
        m.setRegisteredAt(e.getRegisteredAt()); m.setLastUsedAt(e.getLastUsedAt()); m.setDisabledAt(e.getDisabledAt());
        return m;
    }

    private BiometricCredentialEntity toEntity(BiometricCredential m) {
        BiometricCredentialEntity e = new BiometricCredentialEntity();
        e.setId(m.getId()); e.setCredentialId(m.getCredentialId()); e.setRegistrationId(m.getRegistrationId());
        e.setUid(m.getUid()); e.setPlatform(m.getPlatform().name()); e.setPasswordVersion(m.getPasswordVersion());
        e.setSecurityVersion(m.getSecurityVersion()); e.setPublicKey(m.getPublicKey()); e.setAlgorithm(m.getAlgorithm().name());
        e.setKeyAttestationStatus(m.getKeyAttestationStatus().name()); e.setAppIntegrityStatus(m.getAppIntegrityStatus().name());
        e.setStatus(m.getStatus().name()); e.setDisabledReason(m.getDisabledReason() == null ? null : m.getDisabledReason().name());
        e.setRegisteredAt(m.getRegisteredAt()); e.setLastUsedAt(m.getLastUsedAt()); e.setDisabledAt(m.getDisabledAt());
        return e;
    }
}
