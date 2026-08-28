package com.wallet.pay.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wallet.pay.entity.RefundOrder;
import com.wallet.common.enums.RefundOrderState;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;

/**
 * 退款主单 Mapper。全部 default + LambdaWrapper 实现；
 * 状态推进一律条件更新（CAS：WHERE 带原状态，影响行数=1 才算成功）。
 */
@Mapper
public interface RefundOrderMapper extends BaseMapper<RefundOrder> {

    default RefundOrder findByRefundNo(String refundNo) {
        return selectOne(new LambdaQueryWrapper<RefundOrder>()
            .eq(RefundOrder::getRefundNo, refundNo)
            .last("LIMIT 1"));
    }

    /** 条件推进状态 */
    default int changeState(String refundNo, RefundOrderState from, RefundOrderState to) {
        return update(new LambdaUpdateWrapper<RefundOrder>()
            .set(RefundOrder::getState, to)
            .set(RefundOrder::getUpdateTime, LocalDateTime.now())
            .eq(RefundOrder::getRefundNo, refundNo)
            .eq(RefundOrder::getState, from));
    }

    /** 推进到成功并记录完成时间 */
    default int markSuccess(String refundNo, RefundOrderState from, RefundOrderState to, LocalDateTime now) {
        return update(new LambdaUpdateWrapper<RefundOrder>()
            .set(RefundOrder::getState, to)
            .set(RefundOrder::getFinishTime, now)
            .set(RefundOrder::getUpdateTime, LocalDateTime.now())
            .eq(RefundOrder::getRefundNo, refundNo)
            .eq(RefundOrder::getState, from));
    }

    /** 更新退还积分数 */
    default int updateRefundPoint(String refundNo, Long point) {
        return update(new LambdaUpdateWrapper<RefundOrder>()
            .set(RefundOrder::getRefundPoint, point)
            .set(RefundOrder::getUpdateTime, LocalDateTime.now())
            .eq(RefundOrder::getRefundNo, refundNo));
    }

    /** 更新是否返还券标记 */
    default int updateCouponBack(String refundNo, int couponBack) {
        return update(new LambdaUpdateWrapper<RefundOrder>()
            .set(RefundOrder::getCouponBack, couponBack)
            .set(RefundOrder::getUpdateTime, LocalDateTime.now())
            .eq(RefundOrder::getRefundNo, refundNo));
    }
}
