package com.wallet.pay.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wallet.pay.entity.RefundPart;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 退款分段 Mapper。
 */
@Mapper
public interface RefundPartMapper extends BaseMapper<RefundPart> {

    @Select("SELECT * FROM refund_part WHERE refund_part_no = #{refundPartNo} LIMIT 1")
    RefundPart findByRefundPartNo(@Param("refundPartNo") String refundPartNo);

    @Select("SELECT * FROM refund_part WHERE refund_no = #{refundNo} ORDER BY id")
    List<RefundPart> findByRefundNo(@Param("refundNo") String refundNo);

    /** 条件推进状态 */
    @Update("UPDATE refund_part SET state = #{to}, update_time = NOW() "
        + "WHERE refund_part_no = #{refundPartNo} AND state = #{from}")
    int changeState(@Param("refundPartNo") String refundPartNo, @Param("from") String from,
        @Param("to") String to);
}
