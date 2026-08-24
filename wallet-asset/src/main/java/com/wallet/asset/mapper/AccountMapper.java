package com.wallet.asset.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wallet.asset.entity.Account;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 账户 Mapper。余额/积分增减全部条件更新（CAS），影响行数=1 才算成功。
 */
@Mapper
public interface AccountMapper extends BaseMapper<Account> {

    /** 扣余额：余额不足时影响行数为 0 */
    @Update("UPDATE wallet_account SET money = money - #{amount} WHERE user_id = #{userId} AND money >= #{amount}")
    int decreaseMoney(@Param("userId") Long userId, @Param("amount") Long amount);

    /** 加余额：账户存在即可 */
    @Update("UPDATE wallet_account SET money = money + #{amount} WHERE user_id = #{userId}")
    int increaseMoney(@Param("userId") Long userId, @Param("amount") Long amount);

    /** 扣积分：积分不足时影响行数为 0 */
    @Update("UPDATE wallet_account SET point = point - #{count} WHERE user_id = #{userId} AND point >= #{count}")
    int decreasePoint(@Param("userId") Long userId, @Param("count") Long count);

    /** 加积分 */
    @Update("UPDATE wallet_account SET point = point + #{count} WHERE user_id = #{userId}")
    int increasePoint(@Param("userId") Long userId, @Param("count") Long count);

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

    /** 建表时用：给新用户初始化账户（幂等，已存在则忽略） */
    @Insert("INSERT IGNORE INTO wallet_account(user_id, money, point, status) VALUES(#{userId}, 0, 0, 1)")
    int createIfMissing(@Param("userId") Long userId);
}
