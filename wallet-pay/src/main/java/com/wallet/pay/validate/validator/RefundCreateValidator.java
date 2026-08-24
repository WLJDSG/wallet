package com.wallet.pay.validate.validator;

import com.wallet.common.error.BizException;
import com.wallet.pay.error.OrderError;
import com.wallet.pay.state.OrderState;
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
            throw new BizException(OrderError.ORDER_NOT_PAID, context.orderNo());
        }
        long amount = context.refundAmount();
        if (amount <= 0) {
            throw new BizException(OrderError.REFUND_AMOUNT_INVALID, "amount=" + amount);
        }
        if (context.order().getRefundableAmount() < amount) {
            throw new BizException(OrderError.REFUND_TOO_MUCH,
                "申请 " + amount + "，可退 " + context.order().getRefundableAmount());
        }
    }

    @Override
    public int order() {
        return 20;
    }
}
