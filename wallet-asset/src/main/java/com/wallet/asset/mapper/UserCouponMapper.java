package com.wallet.asset.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wallet.asset.entity.UserCoupon;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/**
 * 用户券 Mapper。核销 / 返还全部条件更新（CAS），修正 CRMEB 用 updateById 无并发保护的问题。
 */
@Mapper
public interface UserCouponMapper extends BaseMapper<UserCoupon> {

    /**
     * 核销：仅当券属于该用户、未使用、未过期时成功。
     *
     * @return 影响行数，0 = 券不可用（不存在/不属于/已用/已过期）
     */
    @Update("UPDATE user_coupon SET status = 1, use_order_no = #{orderNo}, use_time = #{now} "
        + "WHERE id = #{id} AND user_id = #{userId} AND status = 0 AND expire_time > #{now}")
    int useCoupon(@Param("id") Long id, @Param("userId") Long userId, @Param("orderNo") String orderNo,
        @Param("now") LocalDateTime now);

    /**
     * 返还（支付未完成时的补偿）：仅当券正被本订单核销、未过期时成功。
     *
     * @return 影响行数，0 = 券已过期或其他状态，调用方按"券不返还只留状态"处理
     */
    @Update("UPDATE user_coupon SET status = 0, use_order_no = NULL, use_time = NULL "
        + "WHERE id = #{id} AND user_id = #{userId} AND status = 1 AND use_order_no = #{orderNo} "
        + "AND expire_time > #{now}")
    int restoreCoupon(@Param("id") Long id, @Param("userId") Long userId, @Param("orderNo") String orderNo,
        @Param("now") LocalDateTime now);
}
