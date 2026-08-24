package com.wallet.asset.service;

import lombok.AllArgsConstructor;
import com.wallet.asset.entity.Account;
import com.wallet.asset.error.AssetError;
import com.wallet.asset.mapper.AccountMapper;
import com.wallet.asset.model.AssetSummary;
import com.wallet.common.error.BizException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 账户服务：首次访问建账户、资产总览。
 */
@Service
@AllArgsConstructor
public class AccountService {

    private final AccountMapper accountMapper;
    private final CouponService couponService;


    /** 确保账户存在（首次访问时创建）。 */
    @Transactional
    public Account ensure(Long userId) {
        accountMapper.createIfMissing(userId);
        Account account = accountMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Account>()
                .eq(Account::getUserId, userId));
        return account;
    }

    /** 资产总览。 */
    public AssetSummary summary(Long userId) {
        Account account = ensure(userId);
        return new AssetSummary(account.getMoney() == null ? 0 : account.getMoney(),
            account.getPoint() == null ? 0 : account.getPoint(),
            couponService.usable(userId));
    }

    /** 充值余额（联调用），返回变动后余额。 */
    @Transactional
    public long recharge(Long userId, long amount, String bizNo) {
        if (accountMapper.increaseMoney(userId, amount) == 0) {
            throw new BizException(AssetError.ACCOUNT_NOT_FOUND, "userId=" + userId);
        }
        return accountMapper.selectMoney(userId);
    }

    /** 发放积分（联调用），返回变动后积分。 */
    @Transactional
    public long addPoint(Long userId, long count) {
        if (accountMapper.increasePoint(userId, count) == 0) {
            throw new BizException(AssetError.ACCOUNT_NOT_FOUND, "userId=" + userId);
        }
        return accountMapper.selectPoint(userId);
    }

    public Long currentMoney(Long userId) {
        Account account = ensure(userId);
        return account.getMoney();
    }

    public LocalDateTime now() {
        return LocalDateTime.now();
    }
}
