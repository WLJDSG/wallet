package com.wallet.pay.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wallet.pay.entity.PayOrder;
import com.wallet.pay.state.OrderState;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 支付主单 Mapper。全部 default + LambdaWrapper 实现；
 * 状态推进一律条件更新（CAS：WHERE 带原状态，影响行数=1 才算成功）。
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
    default int changeState(String orderNo, OrderState from, OrderState to) {
        return update(new LambdaUpdateWrapper<PayOrder>()
            .set(PayOrder::getState, to)
            .set(PayOrder::getUpdateTime, LocalDateTime.now())
            .eq(PayOrder::getOrderNo, orderNo)
            .eq(PayOrder::getState, from));
    }

    /** 推进到支付成功并记录支付时间；可退金额 = 总额 - 券面额（券不折现，永远不可现金退） */
    default int markPaid(String orderNo, OrderState from, OrderState to, LocalDateTime now, long couponAmount) {
        return update(new LambdaUpdateWrapper<PayOrder>()
            .set(PayOrder::getState, to)
            .set(PayOrder::getPayTime, now)
            .setSql("refundable_amount = total_amount - {0}", couponAmount)
            .set(PayOrder::getUpdateTime, LocalDateTime.now())
            .eq(PayOrder::getOrderNo, orderNo)
            .eq(PayOrder::getState, from));
    }

    /** 关闭并记录关闭时间 */
    default int markClosed(String orderNo, OrderState from, OrderState to, LocalDateTime now) {
        return update(new LambdaUpdateWrapper<PayOrder>()
            .set(PayOrder::getState, to)
            .set(PayOrder::getCloseTime, now)
            .set(PayOrder::getUpdateTime, LocalDateTime.now())
            .eq(PayOrder::getOrderNo, orderNo)
            .eq(PayOrder::getState, from));
    }

    /** 推进到失败并记录原因 */
    default int markFailed(String orderNo, OrderState from, OrderState to, String reason) {
        return update(new LambdaUpdateWrapper<PayOrder>()
            .set(PayOrder::getState, to)
            .set(PayOrder::getFailReason, reason)
            .set(PayOrder::getUpdateTime, LocalDateTime.now())
            .eq(PayOrder::getOrderNo, orderNo)
            .eq(PayOrder::getState, from));
    }

    /** 扣减可退金额（CAS：可退充足才成功） */
    default int reduceRefundable(String orderNo, Long amount) {
        return update(new LambdaUpdateWrapper<PayOrder>()
            .setSql("refundable_amount = refundable_amount - {0}", amount)
            .setSql("refunded_amount = refunded_amount + {0}", amount)
            .set(PayOrder::getUpdateTime, LocalDateTime.now())
            .eq(PayOrder::getOrderNo, orderNo)
            .ge(PayOrder::getRefundableAmount, amount));
    }
}
