package com.wallet.asset.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wallet.asset.entity.Coupon;
import com.wallet.asset.entity.UserCoupon;
import com.wallet.asset.error.AssetError;
import com.wallet.asset.mapper.CouponMapper;
import com.wallet.asset.mapper.UserCouponMapper;
import com.wallet.common.error.BizException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 优惠券服务。领券 / 核销 / 返还全部条件更新（CAS）。
 */
@Service
public class CouponService {

    private final CouponMapper couponMapper;
    private final UserCouponMapper userCouponMapper;

    public CouponService(CouponMapper couponMapper, UserCouponMapper userCouponMapper) {
        this.couponMapper = couponMapper;
        this.userCouponMapper = userCouponMapper;
    }

    /** 领券：模板未超发行量才成功。 */
    @Transactional
    public UserCoupon take(Long userId, Long couponId) {
        Coupon coupon = couponMapper.selectById(couponId);
        if (coupon == null || coupon.getStatus() != 1) {
            throw new BizException(AssetError.COUPON_NOT_EXIST, "couponId=" + couponId);
        }
        if (coupon.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new BizException(AssetError.COUPON_EXPIRED, "couponId=" + couponId);
        }
        if (couponMapper.increaseTaken(couponId) == 0) {
            throw new BizException(AssetError.COUPON_SOLD_OUT, "couponId=" + couponId);
        }
        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setUserId(userId);
        userCoupon.setCouponId(couponId);
        userCoupon.setName(coupon.getName());
        userCoupon.setFaceAmount(coupon.getFaceAmount());
        userCoupon.setMinAmount(coupon.getMinAmount());
        userCoupon.setStatus(0);
        userCoupon.setExpireTime(coupon.getExpireTime());
        userCoupon.setCreateTime(LocalDateTime.now());
        userCouponMapper.insert(userCoupon);
        return userCoupon;
    }

    /** 下单前校验券可用（归属/未用/未过期/满足门槛），返回用户券快照。 */
    public UserCoupon checkUsable(Long userId, Long userCouponId, long orderAmount) {
        UserCoupon userCoupon = userCouponMapper.selectById(userCouponId);
        if (userCoupon == null || !userCoupon.getUserId().equals(userId)) {
            throw new BizException(AssetError.COUPON_NOT_OWNED, "userCouponId=" + userCouponId);
        }
        if (userCoupon.getStatus() != 0) {
            throw new BizException(AssetError.COUPON_USED, "userCouponId=" + userCouponId);
        }
        if (userCoupon.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new BizException(AssetError.COUPON_EXPIRED, "userCouponId=" + userCouponId);
        }
        if (userCoupon.getMinAmount() > orderAmount) {
            throw new BizException(AssetError.COUPON_NOT_MATCH,
                "门槛 " + userCoupon.getMinAmount() + " > 订单金额 " + orderAmount);
        }
        return userCoupon;
    }

    /** 核销（条件更新：未使用且未过期才成功）。 */
    @Transactional
    public void use(Long userId, Long userCouponId, String orderNo) {
        int rows = userCouponMapper.useCoupon(userCouponId, userId, orderNo, LocalDateTime.now());
        if (rows == 0) {
            throw new BizException(AssetError.COUPON_USED, "userCouponId=" + userCouponId);
        }
    }

    /**
     * 返还（支付未完成补偿时调用）。券已过期则返回 false，不恢复。
     *
     * @return 是否成功恢复为未使用
     */
    @Transactional
    public boolean restore(Long userId, Long userCouponId, String orderNo) {
        return userCouponMapper.restoreCoupon(userCouponId, userId, orderNo, LocalDateTime.now()) == 1;
    }

    /** 用户可用券列表。 */
    public List<UserCoupon> usable(Long userId) {
        return userCouponMapper.selectList(new LambdaQueryWrapper<UserCoupon>()
            .eq(UserCoupon::getUserId, userId)
            .eq(UserCoupon::getStatus, 0)
            .gt(UserCoupon::getExpireTime, LocalDateTime.now()));
    }
}
