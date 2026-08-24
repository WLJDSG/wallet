package com.wallet.pay.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wallet.pay.entity.PayPart;
import com.wallet.pay.state.PartState;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 支付分段 Mapper。全部 default + LambdaWrapper 实现；
 * 状态推进一律条件更新（CAS：WHERE 带原状态，影响行数=1 才算成功）。
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
    default int changeState(String partNo, PartState from, PartState to) {
        return update(new LambdaUpdateWrapper<PayPart>()
            .set(PayPart::getState, to)
            .set(PayPart::getUpdateTime, LocalDateTime.now())
            .eq(PayPart::getPartNo, partNo)
            .eq(PayPart::getState, from));
    }

    /** 条件推进并写渠道侧交易号、渠道支付参数、支付时间 */
    default int markChannelPaid(String partNo, PartState from, PartState to,
        String thirdNo, String payload, LocalDateTime now) {
        return update(new LambdaUpdateWrapper<PayPart>()
            .set(PayPart::getState, to)
            .set(PayPart::getThirdNo, thirdNo)
            .set(PayPart::getChannelPayload, payload)
            .set(PayPart::getPayTime, now)
            .set(PayPart::getUpdateTime, LocalDateTime.now())
            .eq(PayPart::getPartNo, partNo)
            .eq(PayPart::getState, from));
    }

    /** 资产段支付成功（同步扣成） */
    default int markAssetDone(String partNo, PartState from, PartState to, LocalDateTime now) {
        return update(new LambdaUpdateWrapper<PayPart>()
            .set(PayPart::getState, to)
            .set(PayPart::getPayTime, now)
            .set(PayPart::getUpdateTime, LocalDateTime.now())
            .eq(PayPart::getPartNo, partNo)
            .eq(PayPart::getState, from));
    }

    /** 增加已退金额（CAS：累计已退不超过本段金额） */
    default int increaseRefunded(String partNo, Long amount) {
        return update(new LambdaUpdateWrapper<PayPart>()
            .setSql("refunded_amount = refunded_amount + {0}", amount)
            .set(PayPart::getUpdateTime, LocalDateTime.now())
            .eq(PayPart::getPartNo, partNo)
            .apply("refunded_amount + {0} <= amount", amount));
    }

    /** 条件推进并写渠道侧交易号（回调/查询确认时用） */
    default int changeStateWithThird(String partNo, PartState from, PartState to, String thirdNo) {
        return update(new LambdaUpdateWrapper<PayPart>()
            .set(PayPart::getState, to)
            .set(PayPart::getThirdNo, thirdNo)
            .set(PayPart::getUpdateTime, LocalDateTime.now())
            .eq(PayPart::getPartNo, partNo)
            .eq(PayPart::getState, from));
    }

    /** 写入渠道支付参数（下单成功后前端拉起支付用） */
    default int updatePayload(String partNo, String payload) {
        return update(new LambdaUpdateWrapper<PayPart>()
            .set(PayPart::getChannelPayload, payload)
            .set(PayPart::getUpdateTime, LocalDateTime.now())
            .eq(PayPart::getPartNo, partNo));
    }
}
