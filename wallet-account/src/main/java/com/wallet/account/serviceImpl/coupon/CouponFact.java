package com.wallet.account.serviceImpl.coupon;

import com.wallet.account.entity.UserCoupon;
import com.wallet.common.enums.CouponType;

/**
 * 券规则事实：一张用户券 + 一笔订单。
 *
 * @param coupon      用户券（含类型/面额/折扣率/门槛/封顶快照）
 * @param orderAmount 订单总额，单位分
 */
public record CouponFact(UserCoupon coupon, long orderAmount) {

    /** 券类型（历史数据无类型按满减处理） */
    public CouponType type() {
        return coupon.getType() == null ? CouponType.FULL_CUT : coupon.getType();
    }
}
