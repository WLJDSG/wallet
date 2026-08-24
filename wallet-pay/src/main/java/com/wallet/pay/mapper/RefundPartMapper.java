package com.wallet.pay.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wallet.channel.enums.RefundState;
import com.wallet.pay.entity.RefundPart;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 退款分段 Mapper。
 */
@Mapper
public interface RefundPartMapper extends BaseMapper<RefundPart> {

    default RefundPart findByRefundPartNo(String refundPartNo) {
        return selectOne(new LambdaQueryWrapper<RefundPart>()
            .eq(RefundPart::getRefundPartNo, refundPartNo)
            .last("LIMIT 1"));
    }

    default List<RefundPart> findByRefundNo(String refundNo) {
        return selectList(new LambdaQueryWrapper<RefundPart>()
            .eq(RefundPart::getRefundNo, refundNo)
            .orderByAsc(RefundPart::getId));
    }

    /** 条件推进状态 */
    @Update("UPDATE refund_part SET state = #{to}, update_time = NOW() "
        + "WHERE refund_part_no = #{refundPartNo} AND state = #{from}")
    int changeState(@Param("refundPartNo") String refundPartNo, @Param("from") RefundState from,
        @Param("to") RefundState to);
}
