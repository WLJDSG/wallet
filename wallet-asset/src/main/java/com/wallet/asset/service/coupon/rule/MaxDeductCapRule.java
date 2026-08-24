package com.wallet.asset.service.coupon.rule;

import com.wallet.asset.service.coupon.CouponDeductRule;
import com.wallet.asset.service.coupon.CouponFact;
import org.springframework.stereotype.Component;

/**
 * 封顶规则：最高抵扣金额（max_deduct_amount，>0 时生效，所有券类型通用）。
 */
@Component
public class MaxDeductCapRule implements CouponDeductRule {

    @Override
    public boolean matches(CouponFact fact) {
        Long maxDeduct = fact.coupon().getMaxDeductAmount();
        return maxDeduct != null && maxDeduct > 0;
    }

    @Override
    public Long apply(CouponFact fact, Long current) {
        return Math.min(current, fact.coupon().getMaxDeductAmount());
    }

    @Override
    public int order() {
        return 30;
    }
}
