package com.wallet.pay.validate.validator;

import com.wallet.common.error.CommonException;
import com.wallet.common.error.ErrorCode;
import com.wallet.common.enums.OrderState;
import com.wallet.pay.validate.PayScene;
import com.wallet.pay.validate.PayValidationContext;
import com.wallet.pay.validate.PayValidator;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 退款校验：订单已支付、申请金额为正且不超剩余可退（最终以扣可退 CAS 兜底）。
 */
@Component
public class RefundCreateValidator implements PayValidator {

    @Override
    public Set<PayScene> scenes() {
        return Set.of(PayScene.REFUND_CREATE);
    }

    @Override
    public void validate(PayValidationContext context) {
        if (context.order().getState() != OrderState.SUCCESS) {
            throw new CommonException(ErrorCode.ORDER_NOT_PAID, context.orderNo());
        }
        long amount = context.refundAmount();
        if (amount <= 0) {
            throw new CommonException(ErrorCode.REFUND_AMOUNT_INVALID, "amount=" + amount);
        }
        if (context.order().getRefundableAmount() < amount) {
            throw new CommonException(ErrorCode.REFUND_TOO_MUCH,
                "申请 " + amount + "，可退 " + context.order().getRefundableAmount());
        }
    }

    @Override
    public int order() {
        return 20;
    }
}
