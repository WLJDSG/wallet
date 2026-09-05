package com.wallet.security.testutil;

import com.wallet.security.spi.UserSecuritySettingsRepository;
import com.wallet.security.spi.model.UserSecuritySettings;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 用户安全设置仓储的内存实现。模拟数据库行为：查询返回副本、insert 回填自增 id、
 * 更新按 SPI 契约做版本 CAS 条件更新，避免测试对象别名掩盖遗漏的写库调用。
 */
public class InMemoryUserSecuritySettingsRepository implements UserSecuritySettingsRepository {

    private final List<UserSecuritySettings> rows = new ArrayList<>();
    private final AtomicInteger idSeq = new AtomicInteger();

    @Override
    public synchronized UserSecuritySettings findByUid(Long uid) {
        for (UserSecuritySettings row : rows) {
            if (row.getUid().equals(uid)) {
                return copy(row);
            }
        }
        return null;
    }

    @Override
    public synchronized void insert(UserSecuritySettings settings) {
        settings.setId(idSeq.incrementAndGet());
        rows.add(copy(settings));
    }

    @Override
    public synchronized boolean updateWithVersion(UserSecuritySettings settings, int expectedSecurityVersion) {
        for (UserSecuritySettings row : rows) {
            if (row.getUid().equals(settings.getUid())
                && Integer.valueOf(expectedSecurityVersion).equals(row.getSecurityVersion())) {
                row.setPasswordHash(settings.getPasswordHash());
                row.setPasswordVersion(settings.getPasswordVersion());
                row.setSecurityVersion(settings.getSecurityVersion());
                row.setPasswordStatus(settings.getPasswordStatus());
                row.setPasswordSetAt(settings.getPasswordSetAt());
                row.setPasswordUpdatedAt(settings.getPasswordUpdatedAt());
                return true;
            }
        }
        return false;
    }

    private UserSecuritySettings copy(UserSecuritySettings source) {
        UserSecuritySettings target = new UserSecuritySettings();
        target.setId(source.getId());
        target.setUid(source.getUid());
        target.setPasswordHash(source.getPasswordHash());
        target.setPasswordVersion(source.getPasswordVersion());
        target.setSecurityVersion(source.getSecurityVersion());
        target.setPasswordStatus(source.getPasswordStatus());
        target.setPasswordSetAt(source.getPasswordSetAt());
        target.setPasswordUpdatedAt(source.getPasswordUpdatedAt());
        return target;
    }
}
