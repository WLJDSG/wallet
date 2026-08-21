package com.wallet.pay.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wallet.pay.entity.RefundOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/**
 * 退款主单 Mapper。
 */
@Mapper
public interface RefundOrderMapper extends BaseMapper<RefundOrder> {

    @Select("SELECT * FROM refund_order WHERE refund_no = #{refundNo} LIMIT 1")
    RefundOrder findByRefundNo(@Param("refundNo") String refundNo);

    /** 条件推进状态 */
    @Update("UPDATE refund_order SET state = #{to}, update_time = NOW() "
        + "WHERE refund_no = #{refundNo} AND state = #{from}")
    int changeState(@Param("refundNo") String refundNo, @Param("from") String from, @Param("to") String to);

    /** 推进到成功并记录完成时间 */
    @Update("UPDATE refund_order SET state = #{to}, finish_time = #{now}, update_time = NOW() "
        + "WHERE refund_no = #{refundNo} AND state = #{from}")
    int markSuccess(@Param("refundNo") String refundNo, @Param("from") String from, @Param("to") String to,
        @Param("now") LocalDateTime now);

    @Update("UPDATE refund_order SET refund_point = #{point}, update_time = NOW() "
        + "WHERE refund_no = #{refundNo}")
    int updateRefundPoint(@Param("refundNo") String refundNo, @Param("point") Long point);

    @Update("UPDATE refund_order SET coupon_back = #{couponBack}, update_time = NOW() "
        + "WHERE refund_no = #{refundNo}")
    int updateCouponBack(@Param("refundNo") String refundNo, @Param("couponBack") int couponBack);
}
