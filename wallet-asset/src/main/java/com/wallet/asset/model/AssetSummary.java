package com.wallet.asset.model;

import com.wallet.asset.entity.UserCoupon;

import java.util.List;

/**
 * 资产总览。
 *
 * @param money         余额，单位分
 * @param point         积分数量
 * @param usableCoupons 可用券列表
 */
public record AssetSummary(long money, long point, List<UserCoupon> usableCoupons) {
}
