package com.wallet.app.model;

import io.swagger.v3.oas.annotations.media.Schema;
import com.wallet.common.enums.PayType;
import com.wallet.contract.pay.model.PartItem;
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
@Schema(description = "支付分段")
public record PartItemReq(
    @Schema(description = "分段类型：COUPON 券 / POINT 积分 / MONEY 余额 / CHANNEL 三方渠道")
    @NotNull(message = "分段类型不能为空") PayType payType,
    @Schema(description = "本段抵扣金额，单位分", example = "3000")
    @Positive(message = "分段金额必须大于 0") long amount,
    @Schema(description = "券段必填：用户券 ID") Long userCouponId,
    @Schema(description = "积分段必填：消耗积分数") Long pointCount,
    @Schema(description = "三方段必填：渠道编码", example = "MOCK") String channelCode) {

    public PartItem toCmd() {
        return new PartItem(payType, amount, userCouponId, pointCount, channelCode);
    }
}
