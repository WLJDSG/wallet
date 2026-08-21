package com.wallet.pay.model;

/**
 * 创建支付单时的分段请求。
 *
 * @param payType       COUPON 券 / POINT 积分 / MONEY 余额 / CHANNEL 三方渠道
 * @param amount        本段抵扣金额，单位分
 * @param userCouponId  券段必填：用户券 ID
 * @param pointCount    积分段必填：消耗积分数
 * @param channelCode   三方段必填：渠道编码
 */
public record PartItem(String payType, long amount, Long userCouponId, Long pointCount, String channelCode) {
}
