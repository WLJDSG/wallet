package com.wallet.pay.validate.validator;

import com.wallet.common.error.BizException;
import com.wallet.pay.enums.PayType;
import com.wallet.pay.error.OrderError;
import com.wallet.pay.model.PartItem;
import com.wallet.pay.validate.PayScene;
import com.wallet.pay.validate.PayValidationContext;
import com.wallet.pay.validate.PayValidator;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 创建校验：金额勾稽（sum(段金额)=总额）+ 至多一个三方分段。
 */
@Component
public class CreateAmountValidator implements PayValidator {

    @Override
    public Set<PayScene> scenes() {
        return Set.of(PayScene.CREATE);
    }

    @Override
    public void validate(PayValidationContext context) {
        long sum = 0;
        int channelCount = 0;
        for (PartItem item : context.cmd().parts()) {
            sum += item.amount();
            if (item.payType() == PayType.CHANNEL) {
                channelCount++;
            }
        }
        if (sum != context.cmd().totalAmount()) {
            throw new BizException(OrderError.AMOUNT_NOT_MATCH,
                "sum=" + sum + ", total=" + context.cmd().totalAmount());
        }
        if (channelCount > 1) {
            throw new BizException(OrderError.PART_INVALID, "一个支付单至多一个三方分段");
        }
    }

    @Override
    public int order() {
        return 20;
    }
}
