package com.wallet.pay.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wallet.contract.channel.enums.RefundState;
import com.wallet.pay.entity.RefundPart;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 退款分段 Mapper。全部 default + LambdaWrapper 实现；
 * 状态推进一律条件更新（CAS：WHERE 带原状态，影响行数=1 才算成功）。
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
    default int changeState(String refundPartNo, RefundState from, RefundState to) {
        return update(new LambdaUpdateWrapper<RefundPart>()
            .set(RefundPart::getState, to)
            .set(RefundPart::getUpdateTime, LocalDateTime.now())
            .eq(RefundPart::getRefundPartNo, refundPartNo)
            .eq(RefundPart::getState, from));
    }
}
