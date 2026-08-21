package com.wallet.asset.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wallet.asset.entity.MoneyLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 余额流水 Mapper。biz_no + type 唯一索引保证幂等。
 */
@Mapper
public interface MoneyLogMapper extends BaseMapper<MoneyLog> {

    @Select("SELECT * FROM money_log WHERE biz_no = #{bizNo} AND type = #{type} LIMIT 1")
    MoneyLog findByBizAndType(@Param("bizNo") String bizNo, @Param("type") String type);

    /** 重复写入被唯一索引静默忽略 */
    @Insert("INSERT IGNORE INTO money_log(user_id, biz_no, type, change_amount, after_amount, order_no, remark) "
        + "VALUES(#{userId}, #{bizNo}, #{type}, #{changeAmount}, #{afterAmount}, #{orderNo}, #{remark})")
    int insertIgnore(MoneyLog log);
}
