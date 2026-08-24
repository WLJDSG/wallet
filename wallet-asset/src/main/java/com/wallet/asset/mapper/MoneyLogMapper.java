package com.wallet.asset.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wallet.asset.entity.MoneyLog;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.dao.DuplicateKeyException;

/**
 * 余额流水 Mapper。全部 default + LambdaWrapper 实现；biz_no + type 唯一索引保证幂等。
 */
@Mapper
public interface MoneyLogMapper extends BaseMapper<MoneyLog> {

    default MoneyLog findByBizAndType(String bizNo, String type) {
        return selectOne(new LambdaQueryWrapper<MoneyLog>()
            .eq(MoneyLog::getBizNo, bizNo)
            .eq(MoneyLog::getType, type)
            .last("LIMIT 1"));
    }

    /** 幂等写入：biz_no + type 唯一索引命中时静默忽略（等价原 INSERT IGNORE） */
    default int insertIgnore(MoneyLog moneyLog) {
        try {
            return insert(moneyLog);
        } catch (DuplicateKeyException e) {
            return 0;
        }
    }
}
