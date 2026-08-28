package com.wallet.pay.validate.validator;

import com.wallet.common.error.CommonException;
import com.wallet.common.error.ErrorCode;
import com.wallet.pay.validate.PayScene;
import com.wallet.pay.validate.PayValidationContext;
import com.wallet.pay.validate.PayValidator;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 归属校验：订单存在且属于当前用户（除创建外的所有场景，链上第一个执行）。
 */
@Component
public class OrderOwnershipValidator implements PayValidator {

    @Override
    public Set<PayScene> scenes() {
        return Set.of(PayScene.SUBMIT, PayScene.DETAIL, PayScene.QUERY, PayScene.CANCEL,
            PayScene.REFUND_CREATE);
    }

    @Override
    public void validate(PayValidationContext context) {
        if (context.order() == null) {
            throw new CommonException(ErrorCode.ORDER_NOT_FOUND, context.orderNo());
        }
        if (!context.order().getUserId().equals(context.userId())) {
            throw new CommonException(ErrorCode.ORDER_NOT_OWNED, context.orderNo());
        }
    }

    @Override
    public int order() {
        return 10;
    }
}
