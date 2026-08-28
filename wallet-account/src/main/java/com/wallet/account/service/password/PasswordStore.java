package com.wallet.account.service.password;

import com.wallet.account.entity.PayPassword;

/**
 * 支付密码持久化契约。拆出接口便于单测（测试用内存实现）。
 */
public interface PasswordStore {

    PayPassword findByUserId(Long userId);

    void insert(PayPassword password);

    void updateHash(Long userId, String newHash, int newVersion);
}
