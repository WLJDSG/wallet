package com.wallet.asset.service.password;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.wallet.asset.entity.PayPassword;
import com.wallet.asset.mapper.PayPasswordMapper;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 基于 MyBatis-Plus 的支付密码存储实现。
 */
@Component
public class PasswordStoreImpl implements PasswordStore {

    private final PayPasswordMapper mapper;

    public PasswordStoreImpl(PayPasswordMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public PayPassword findByUserId(Long userId) {
        return mapper.selectOne(new LambdaQueryWrapper<PayPassword>().eq(PayPassword::getUserId, userId));
    }

    @Override
    public void insert(PayPassword password) {
        LocalDateTime now = LocalDateTime.now();
        password.setCreateTime(now);
        password.setUpdateTime(now);
        mapper.insert(password);
    }

    @Override
    public void updateHash(Long userId, String newHash, int newVersion) {
        mapper.update(null, new LambdaUpdateWrapper<PayPassword>()
            .eq(PayPassword::getUserId, userId)
            .set(PayPassword::getPasswordHash, newHash)
            .set(PayPassword::getVersion, newVersion)
            .set(PayPassword::getUpdateTime, LocalDateTime.now()));
    }
}
