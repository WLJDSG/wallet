package com.wallet.account.security;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.wallet.account.entity.PayPassword;
import com.wallet.account.mapper.PayPasswordMapper;
import com.wallet.security.enums.PayPasswordStatusEnum;
import com.wallet.security.spi.UserSecuritySettingsRepository;
import com.wallet.security.spi.model.UserSecuritySettings;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

/** 将钱包现有 pay_password 表适配为完整支付安全设置仓储。 */
@Component
@AllArgsConstructor
public class UserSecuritySettingsAdapter implements UserSecuritySettingsRepository {

    private final PayPasswordMapper mapper;

    @Override
    public UserSecuritySettings findByUid(Long uid) {
        PayPassword entity = mapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PayPassword>()
            .eq(PayPassword::getUserId, uid));
        if (entity == null) {
            return null;
        }
        UserSecuritySettings model = new UserSecuritySettings();
        model.setId(entity.getId().intValue());
        model.setUid(entity.getUserId());
        model.setPasswordHash(entity.getPasswordHash());
        model.setPasswordVersion(entity.getVersion());
        model.setSecurityVersion(entity.getSecurityVersion());
        model.setPasswordStatus(PayPasswordStatusEnum.valueOf(entity.getStatus()));
        model.setPasswordSetAt(entity.getPasswordSetAt());
        model.setPasswordUpdatedAt(entity.getPasswordUpdatedAt());
        return model;
    }

    @Override
    public void insert(UserSecuritySettings settings) {
        PayPassword entity = toEntity(settings);
        mapper.insert(entity);
        settings.setId(entity.getId().intValue());
    }

    @Override
    public boolean updateWithVersion(UserSecuritySettings settings, int expectedSecurityVersion) {
        return mapper.update(new LambdaUpdateWrapper<PayPassword>()
            .eq(PayPassword::getUserId, settings.getUid())
            .eq(PayPassword::getSecurityVersion, expectedSecurityVersion)
            .set(PayPassword::getPasswordHash, settings.getPasswordHash())
            .set(PayPassword::getVersion, settings.getPasswordVersion())
            .set(PayPassword::getSecurityVersion, settings.getSecurityVersion())
            .set(PayPassword::getStatus, settings.getPasswordStatus().name())
            .set(PayPassword::getPasswordSetAt, settings.getPasswordSetAt())
            .set(PayPassword::getPasswordUpdatedAt, settings.getPasswordUpdatedAt())) == 1;
    }

    private PayPassword toEntity(UserSecuritySettings settings) {
        PayPassword entity = new PayPassword();
        entity.setUserId(settings.getUid());
        entity.setPasswordHash(settings.getPasswordHash());
        entity.setVersion(settings.getPasswordVersion());
        entity.setSecurityVersion(settings.getSecurityVersion());
        entity.setStatus(settings.getPasswordStatus().name());
        entity.setPasswordSetAt(settings.getPasswordSetAt());
        entity.setPasswordUpdatedAt(settings.getPasswordUpdatedAt());
        return entity;
    }
}
