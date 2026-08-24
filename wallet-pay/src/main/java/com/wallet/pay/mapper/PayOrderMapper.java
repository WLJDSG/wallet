package com.wallet.pay.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wallet.pay.entity.PayOrder;
import com.wallet.pay.state.OrderState;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 支付主单 Mapper。状态推进全部条件更新（CAS）。
 */
@Mapper
public interface PayOrderMapper extends BaseMapper<PayOrder> {

    default PayOrder findByOrderNo(String orderNo) {
        return selectOne(new LambdaQueryWrapper<PayOrder>()
            .eq(PayOrder::getOrderNo, orderNo)
            .last("LIMIT 1"));
    }

    /** 超时关单任务扫描：INIT/PAYING 且已过期 */
    default List<PayOrder> findExpired(LocalDateTime now, int limit) {
        return selectList(new LambdaQueryWrapper<PayOrder>()
            .in(PayOrder::getState, OrderState.INIT, OrderState.PAYING)
            .lt(PayOrder::getExpireTime, now)
            .orderByAsc(PayOrder::getId)
            .last("LIMIT " + limit));
    }

    /** 条件推进状态 */
    @Update("UPDATE pay_order SET state = #{to}, update_time = NOW() "
        + "WHERE order_no = #{orderNo} AND state = #{from}")
    int changeState(@Param("orderNo") String orderNo, @Param("from") OrderState from, @Param("to") OrderState to);

    /** 推进到支付成功并记录支付时间；可退金额 = 总额 - 券面额（券不折现，永远不可现金退） */
    @Update("UPDATE pay_order SET state = #{to}, pay_time = #{now}, "
        + "refundable_amount = total_amount - #{couponAmount}, update_time = NOW() "
        + "WHERE order_no = #{orderNo} AND state = #{from}")
    int markPaid(@Param("orderNo") String orderNo, @Param("from") OrderState from, @Param("to") OrderState to,
        @Param("now") LocalDateTime now, @Param("couponAmount") long couponAmount);

    /** 关闭并记录关闭时间 */
    @Update("UPDATE pay_order SET state = #{to}, close_time = #{now}, update_time = NOW() "
        + "WHERE order_no = #{orderNo} AND state = #{from}")
    int markClosed(@Param("orderNo") String orderNo, @Param("from") OrderState from, @Param("to") OrderState to,
        @Param("now") LocalDateTime now);

    /** 推进到失败并记录原因 */
    @Update("UPDATE pay_order SET state = #{to}, fail_reason = #{reason}, update_time = NOW() "
        + "WHERE order_no = #{orderNo} AND state = #{from}")
    int markFailed(@Param("orderNo") String orderNo, @Param("from") OrderState from, @Param("to") OrderState to,
        @Param("reason") String reason);

    /** 扣减可退金额（CAS：可退充足才成功） */
    @Update("UPDATE pay_order SET refundable_amount = refundable_amount - #{amount}, "
        + "refunded_amount = refunded_amount + #{amount}, update_time = NOW() "
        + "WHERE order_no = #{orderNo} AND refundable_amount >= #{amount}")
    int reduceRefundable(@Param("orderNo") String orderNo, @Param("amount") Long amount);
}
