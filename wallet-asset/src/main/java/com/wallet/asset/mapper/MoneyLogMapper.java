package com.wallet.asset.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wallet.asset.entity.MoneyLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

/**
 * 余额流水 Mapper。biz_no + type 唯一索引保证幂等。
 */
@Mapper
public interface MoneyLogMapper extends BaseMapper<MoneyLog> {

    default MoneyLog findByBizAndType(String bizNo, String type) {
        return selectOne(new LambdaQueryWrapper<MoneyLog>()
            .eq(MoneyLog::getBizNo, bizNo)
            .eq(MoneyLog::getType, type)
            .last("LIMIT 1"));
    }

    /** 重复写入被唯一索引静默忽略 */
    @Insert("INSERT IGNORE INTO money_log(user_id, biz_no, type, change_amount, after_amount, order_no, remark) "
        + "VALUES(#{userId}, #{bizNo}, #{type}, #{changeAmount}, #{afterAmount}, #{orderNo}, #{remark})")
    int insertIgnore(MoneyLog log);
}
