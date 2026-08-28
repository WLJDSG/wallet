package com.wallet.contract.pay.model;

import com.wallet.contract.pay.enums.PayType;

/**
 * 创建支付单时的分段（服务层入参，基础校验在 wallet-app 的 PartItemReq）。
 * 分段类型相关的条件必填（券 ID/积分数/渠道编码）在服务层校验。
 *
 * @param payType       分段类型
 * @param amount        本段抵扣金额，单位分
 * @param userCouponId  券段必填：用户券 ID
 * @param pointCount    积分段必填：消耗积分数
 * @param channelCode   三方段必填：渠道编码
 */
public record PartItem(PayType payType, long amount, Long userCouponId, Long pointCount, String channelCode) {
}
