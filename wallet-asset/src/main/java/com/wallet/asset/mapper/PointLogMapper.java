package com.wallet.asset.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wallet.asset.entity.PointLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 积分流水 Mapper。biz_no + type 唯一索引保证幂等。
 */
@Mapper
public interface PointLogMapper extends BaseMapper<PointLog> {

    @Select("SELECT * FROM point_log WHERE biz_no = #{bizNo} AND type = #{type} LIMIT 1")
    PointLog findByBizAndType(@Param("bizNo") String bizNo, @Param("type") String type);

    @Insert("INSERT IGNORE INTO point_log(user_id, biz_no, type, change_count, after_count, order_no, remark) "
        + "VALUES(#{userId}, #{bizNo}, #{type}, #{changeCount}, #{afterCount}, #{orderNo}, #{remark})")
    int insertIgnore(PointLog log);
}
