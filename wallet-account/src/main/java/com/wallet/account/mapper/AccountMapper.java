package com.wallet.account.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wallet.account.entity.Account;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.dao.DuplicateKeyException;

/**
 * 账户 Mapper。全部 default + LambdaWrapper 实现；
 * 余额/积分增减全部条件更新（CAS），影响行数=1 才算成功。
 */
@Mapper
public interface AccountMapper extends BaseMapper<Account> {

    /** 扣余额：余额不足时影响行数为 0 */
    default int decreaseMoney(Long userId, Long amount) {
        return update(new LambdaUpdateWrapper<Account>()
            .setSql("money = money - {0}", amount)
            .eq(Account::getUserId, userId)
            .ge(Account::getMoney, amount));
    }

    /** 加余额：账户存在即可 */
    default int increaseMoney(Long userId, Long amount) {
        return update(new LambdaUpdateWrapper<Account>()
            .setSql("money = money + {0}", amount)
            .eq(Account::getUserId, userId));
    }

    /** 扣积分：积分不足时影响行数为 0 */
    default int decreasePoint(Long userId, Long count) {
        return update(new LambdaUpdateWrapper<Account>()
            .setSql("point = point - {0}", count)
            .eq(Account::getUserId, userId)
            .ge(Account::getPoint, count));
    }

    /** 加积分 */
    default int increasePoint(Long userId, Long count) {
        return update(new LambdaUpdateWrapper<Account>()
            .setSql("point = point + {0}", count)
            .eq(Account::getUserId, userId));
    }

    default Long selectMoney(Long userId) {
        Account account = selectOne(new LambdaQueryWrapper<Account>()
            .select(Account::getMoney)
            .eq(Account::getUserId, userId));
        return account == null ? null : account.getMoney();
    }

    default Long selectPoint(Long userId) {
        Account account = selectOne(new LambdaQueryWrapper<Account>()
            .select(Account::getPoint)
            .eq(Account::getUserId, userId));
        return account == null ? null : account.getPoint();
    }

    /** 给新用户初始化账户（幂等：user_id 唯一索引兜底，并发重复创建静默忽略） */
    default int createIfMissing(Long userId) {
        if (exists(new LambdaQueryWrapper<Account>().eq(Account::getUserId, userId))) {
            return 0;
        }
        Account account = new Account();
        account.setUserId(userId);
        account.setMoney(0L);
        account.setPoint(0L);
        account.setStatus(1);
        try {
            return insert(account);
        } catch (DuplicateKeyException e) {
            return 0; // 并发下已被其他请求创建
        }
    }
}
