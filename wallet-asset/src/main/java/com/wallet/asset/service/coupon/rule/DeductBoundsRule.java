package com.wallet.asset.service.coupon.rule;

import com.wallet.asset.error.AssetError;
import com.wallet.asset.service.coupon.CouponDeductRule;
import com.wallet.asset.service.coupon.CouponFact;
import com.wallet.common.error.BizException;
import org.springframework.stereotype.Component;

/**
 * 兜底规则：抵扣额不超订单额，且必须为正（为 0 说明该券在本单不可用）。
 */
@Component
public class DeductBoundsRule implements CouponDeductRule {

    @Override
    public boolean matches(CouponFact fact) {
        return true;
    }

    @Override
    public Long apply(CouponFact fact, Long current) {
        long deduct = Math.min(current, fact.orderAmount());
        if (deduct <= 0) {
            throw new BizException(AssetError.COUPON_NOT_MATCH, "该券在本单抵扣额为 0，不可用");
        }
        return deduct;
    }

    @Override
    public int order() {
        return 40;
    }
}
