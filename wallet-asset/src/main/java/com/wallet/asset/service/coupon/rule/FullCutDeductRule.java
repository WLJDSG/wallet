package com.wallet.asset.service.coupon.rule;

import com.wallet.asset.enums.CouponType;
import com.wallet.asset.service.coupon.CouponDeductRule;
import com.wallet.asset.service.coupon.CouponFact;
import org.springframework.stereotype.Component;

/**
 * 计算规则：满减券抵扣额 = 面额。
 */
@Component
public class FullCutDeductRule implements CouponDeductRule {

    @Override
    public boolean matches(CouponFact fact) {
        return fact.type() == CouponType.FULL_CUT;
    }

    @Override
    public Long apply(CouponFact fact, Long current) {
        Long faceAmount = fact.coupon().getFaceAmount();
        return faceAmount == null ? 0L : faceAmount;
    }

    @Override
    public int order() {
        return 20;
    }
}
