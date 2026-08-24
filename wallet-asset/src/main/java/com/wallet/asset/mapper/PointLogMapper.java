package com.wallet.asset.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wallet.asset.entity.PointLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

/**
 * 积分流水 Mapper。biz_no + type 唯一索引保证幂等。
 */
@Mapper
public interface PointLogMapper extends BaseMapper<PointLog> {

    default PointLog findByBizAndType(String bizNo, String type) {
        return selectOne(new LambdaQueryWrapper<PointLog>()
            .eq(PointLog::getBizNo, bizNo)
            .eq(PointLog::getType, type)
            .last("LIMIT 1"));
    }

    @Insert("INSERT IGNORE INTO point_log(user_id, biz_no, type, change_count, after_count, order_no, remark) "
        + "VALUES(#{userId}, #{bizNo}, #{type}, #{changeCount}, #{afterCount}, #{orderNo}, #{remark})")
    int insertIgnore(PointLog log);
}
