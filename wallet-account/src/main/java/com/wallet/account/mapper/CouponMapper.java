package com.wallet.account.mapper;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wallet.account.entity.Coupon;
import org.apache.ibatis.annotations.Mapper;

/**
 * 券模板 Mapper。全部 default + LambdaWrapper 实现；领取量 CAS 自增。
 */
@Mapper
public interface CouponMapper extends BaseMapper<Coupon> {

    /**
     * 领取量 +1：只在未超发行量时成功（total_count=0 表示不限量）。
     *
     * @return 影响行数，0 = 已领完
     */
    default int increaseTaken(Long couponId) {
        return update(new LambdaUpdateWrapper<Coupon>()
            .setSql("taken_count = taken_count + 1")
            .eq(Coupon::getId, couponId)
            .eq(Coupon::getStatus, 1)
            .and(w -> w.eq(Coupon::getTotalCount, 0)
                .or().apply("taken_count < total_count")));
    }
}
