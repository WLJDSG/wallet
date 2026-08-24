package com.wallet.app.model;

import com.wallet.pay.enums.PayType;
import com.wallet.pay.model.PartItem;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * 创建支付单的分段入参。payType 由 Jackson 反序列化为枚举（大小写不敏感，非法值 400）；
 * 分段类型相关的条件必填（券 ID/积分数/渠道编码）在服务层校验。
 *
 * @param payType       COUPON 券 / POINT 积分 / MONEY 余额 / CHANNEL 三方渠道
 * @param amount        本段抵扣金额，单位分
 * @param userCouponId  券段必填：用户券 ID
 * @param pointCount    积分段必填：消耗积分数
 * @param channelCode   三方段必填：渠道编码
 */
public record PartItemReq(@NotNull(message = "分段类型不能为空") PayType payType,
                          @Positive(message = "分段金额必须大于 0") long amount,
                          Long userCouponId, Long pointCount, String channelCode) {

    public PartItem toCmd() {
        return new PartItem(payType, amount, userCouponId, pointCount, channelCode);
    }
}
