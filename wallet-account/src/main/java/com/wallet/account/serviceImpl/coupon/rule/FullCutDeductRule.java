package com.wallet.account.serviceImpl.coupon.rule;

import com.wallet.common.enums.CouponType;
import com.wallet.account.serviceImpl.coupon.CouponDeductRule;
import com.wallet.account.serviceImpl.coupon.CouponFact;
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
