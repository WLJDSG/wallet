package com.wallet.pay.validate.validator;

import com.wallet.contract.account.CouponService;
import com.wallet.common.error.CommonException;
import com.wallet.pay.config.PayProperties;
import com.wallet.common.error.ErrorCode;
import com.wallet.contract.pay.model.PartItem;
import com.wallet.pay.validate.PayScene;
import com.wallet.pay.validate.PayValidationContext;
import com.wallet.pay.validate.PayValidator;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 创建校验：逐分段的条件必填与金额规则——
 * 券段金额必须等于规则引擎计算的抵扣额（满减/折扣/门槛/封顶），
 * 积分段按汇率换算，三方段必须带渠道编码。
 */
@Component
@AllArgsConstructor
public class CreatePartValidator implements PayValidator {

    private final CouponService couponService;
    private final PayProperties payProperties;

    @Override
    public Set<PayScene> scenes() {
        return Set.of(PayScene.CREATE);
    }

    @Override
    public void validate(PayValidationContext context) {
        long totalAmount = context.cmd().totalAmount();
        for (PartItem item : context.cmd().parts()) {
            switch (item.payType()) {
                case COUPON -> {
                    if (item.userCouponId() == null) {
                        throw new CommonException(ErrorCode.PART_INVALID, "券段缺少 userCouponId");
                    }
                    long deduct = couponService.evaluateDeduct(context.userId(), item.userCouponId(),
                        totalAmount);
                    if (deduct != item.amount()) {
                        throw new CommonException(ErrorCode.PART_INVALID, "券段金额应为 " + deduct);
                    }
                }
                case POINT -> {
                    if (item.pointCount() == null || item.pointCount() <= 0) {
                        throw new CommonException(ErrorCode.PART_INVALID, "积分段缺少 pointCount");
                    }
                    long expectAmount = item.pointCount() * 100 / payProperties.getPointsPerYuan();
                    if (expectAmount != item.amount()) {
                        throw new CommonException(ErrorCode.PART_INVALID,
                            "积分段金额应为 " + expectAmount + "（" + item.pointCount() + " 积分按 "
                                + payProperties.getPointsPerYuan() + " 积分/元折算）");
                    }
                }
                case MONEY -> {
                    // 无需额外校验
                }
                case CHANNEL -> {
                    if (item.channelCode() == null || item.channelCode().trim().isEmpty()) {
                        throw new CommonException(ErrorCode.PART_INVALID, "三方段缺少 channelCode");
                    }
                }
            }
        }
    }

    @Override
    public int order() {
        return 30;
    }
}
