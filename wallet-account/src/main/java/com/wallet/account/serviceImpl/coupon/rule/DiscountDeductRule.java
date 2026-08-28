package com.wallet.account.serviceImpl.coupon.rule;

import com.wallet.common.enums.CouponType;
import com.wallet.account.serviceImpl.coupon.CouponDeductRule;
import com.wallet.account.serviceImpl.coupon.CouponFact;
import org.springframework.stereotype.Component;

/**
 * 计算规则：折扣券抵扣额 = 订单额 × (100 - 折扣率) / 100。
 * 如 discount_rate=85（八五折）→ 抵扣订单额的 15%；封顶由 MaxDeductCapRule 处理。
 */
@Component
public class DiscountDeductRule implements CouponDeductRule {

    @Override
    public boolean matches(CouponFact fact) {
        return fact.type() == CouponType.DISCOUNT;
    }

    @Override
    public Long apply(CouponFact fact, Long current) {
        Integer rate = fact.coupon().getDiscountRate();
        if (rate == null || rate <= 0 || rate >= 100) {
            return 0L; // 非法折扣率视为不抵扣，DeductBoundsRule 会拒绝
        }
        return fact.orderAmount() * (100 - rate) / 100;
    }

    @Override
    public int order() {
        return 20;
    }
}
