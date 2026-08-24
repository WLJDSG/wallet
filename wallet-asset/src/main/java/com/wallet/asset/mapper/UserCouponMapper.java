package com.wallet.asset.mapper;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wallet.asset.entity.UserCoupon;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;

/**
 * 用户券 Mapper。全部 default + LambdaWrapper 实现；
 * 核销 / 返还全部条件更新（CAS），修正 CRMEB 用 updateById 无并发保护的问题。
 */
@Mapper
public interface UserCouponMapper extends BaseMapper<UserCoupon> {

    /**
     * 核销：仅当券属于该用户、未使用、未过期时成功。
     *
     * @return 影响行数，0 = 券不可用（不存在/不属于/已用/已过期）
     */
    default int useCoupon(Long id, Long userId, String orderNo, LocalDateTime now) {
        return update(new LambdaUpdateWrapper<UserCoupon>()
            .set(UserCoupon::getStatus, 1)
            .set(UserCoupon::getUseOrderNo, orderNo)
            .set(UserCoupon::getUseTime, now)
            .eq(UserCoupon::getId, id)
            .eq(UserCoupon::getUserId, userId)
            .eq(UserCoupon::getStatus, 0)
            .gt(UserCoupon::getExpireTime, now));
    }

    /**
     * 返还（支付未完成时的补偿）：仅当券正被本订单核销、未过期时成功。
     *
     * @return 影响行数，0 = 券已过期或其他状态，调用方按"券不返还只留状态"处理
     */
    default int restoreCoupon(Long id, Long userId, String orderNo, LocalDateTime now) {
        return update(new LambdaUpdateWrapper<UserCoupon>()
            .set(UserCoupon::getStatus, 0)
            .set(UserCoupon::getUseOrderNo, null)
            .set(UserCoupon::getUseTime, null)
            .eq(UserCoupon::getId, id)
            .eq(UserCoupon::getUserId, userId)
            .eq(UserCoupon::getStatus, 1)
            .eq(UserCoupon::getUseOrderNo, orderNo)
            .gt(UserCoupon::getExpireTime, now));
    }
}
