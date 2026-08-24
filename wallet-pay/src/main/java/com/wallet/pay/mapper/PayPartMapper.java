package com.wallet.pay.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wallet.pay.entity.PayPart;
import com.wallet.pay.state.PartState;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 支付分段 Mapper。状态推进全部条件更新（CAS）。
 */
@Mapper
public interface PayPartMapper extends BaseMapper<PayPart> {

    default PayPart findByPartNo(String partNo) {
        return selectOne(new LambdaQueryWrapper<PayPart>()
            .eq(PayPart::getPartNo, partNo)
            .last("LIMIT 1"));
    }

    default List<PayPart> findByOrderNo(String orderNo) {
        return selectList(new LambdaQueryWrapper<PayPart>()
            .eq(PayPart::getOrderNo, orderNo)
            .orderByAsc(PayPart::getId));
    }

    /** 条件推进状态 */
    @Update("UPDATE pay_part SET state = #{to}, update_time = NOW() "
        + "WHERE part_no = #{partNo} AND state = #{from}")
    int changeState(@Param("partNo") String partNo, @Param("from") PartState from, @Param("to") PartState to);

    /** 条件推进并写渠道侧交易号、渠道支付参数、支付时间 */
    @Update("UPDATE pay_part SET state = #{to}, third_no = #{thirdNo}, channel_payload = #{payload}, "
        + "pay_time = #{now}, update_time = NOW() WHERE part_no = #{partNo} AND state = #{from}")
    int markChannelPaid(@Param("partNo") String partNo, @Param("from") PartState from, @Param("to") PartState to,
        @Param("thirdNo") String thirdNo, @Param("payload") String payload, @Param("now") LocalDateTime now);

    /** 资产段支付成功（同步扣成） */
    @Update("UPDATE pay_part SET state = #{to}, pay_time = #{now}, update_time = NOW() "
        + "WHERE part_no = #{partNo} AND state = #{from}")
    int markAssetDone(@Param("partNo") String partNo, @Param("from") PartState from, @Param("to") PartState to,
        @Param("now") LocalDateTime now);

    /** 增加已退金额（CAS：累计已退不超过本段金额） */
    @Update("UPDATE pay_part SET refunded_amount = refunded_amount + #{amount}, update_time = NOW() "
        + "WHERE part_no = #{partNo} AND refunded_amount + #{amount} <= amount")
    int increaseRefunded(@Param("partNo") String partNo, @Param("amount") Long amount);

    /** 条件推进并写渠道侧交易号（回调/查询确认时用） */
    @Update("UPDATE pay_part SET state = #{to}, third_no = #{thirdNo}, update_time = NOW() "
        + "WHERE part_no = #{partNo} AND state = #{from}")
    int changeStateWithThird(@Param("partNo") String partNo, @Param("from") PartState from,
        @Param("to") PartState to, @Param("thirdNo") String thirdNo);

    /** 写入渠道支付参数（下单成功后前端拉起支付用） */
    @Update("UPDATE pay_part SET channel_payload = #{payload}, update_time = NOW() "
        + "WHERE part_no = #{partNo}")
    int updatePayload(@Param("partNo") String partNo, @Param("payload") String payload);
}
