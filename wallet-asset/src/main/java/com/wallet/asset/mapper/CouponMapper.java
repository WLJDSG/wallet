package com.wallet.asset.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wallet.asset.entity.Coupon;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 券模板 Mapper。领取量 CAS 自增。
 */
@Mapper
public interface CouponMapper extends BaseMapper<Coupon> {

    /**
     * 领取量 +1：只在未超发行量时成功（total_count=0 表示不限量）。
     *
     * @return 影响行数，0 = 已领完
     */
    @Update("UPDATE coupon SET taken_count = taken_count + 1 "
        + "WHERE id = #{couponId} AND status = 1 AND (total_count = 0 OR taken_count < total_count)")
    int increaseTaken(@Param("couponId") Long couponId);
}
