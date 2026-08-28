package com.wallet.account.serviceImpl.coupon.rule;

import com.wallet.common.error.ErrorCode;
import com.wallet.account.serviceImpl.coupon.CouponDeductRule;
import com.wallet.account.serviceImpl.coupon.CouponFact;
import com.wallet.common.error.CommonException;
import org.springframework.stereotype.Component;

/**
 * 校验规则：最低消费门槛（min_amount，所有券类型通用）。
 */
@Component
public class MinAmountRule implements CouponDeductRule {

    @Override
    public boolean matches(CouponFact fact) {
        Long minAmount = fact.coupon().getMinAmount();
        return minAmount != null && minAmount > 0;
    }

    @Override
    public Long apply(CouponFact fact, Long current) {
        if (fact.coupon().getMinAmount() > fact.orderAmount()) {
            throw new CommonException(ErrorCode.COUPON_NOT_MATCH,
                "未达最低消费：门槛 " + fact.coupon().getMinAmount() + " > 订单额 " + fact.orderAmount());
        }
        return current;
    }

    @Override
    public int order() {
        return 10;
    }
}
