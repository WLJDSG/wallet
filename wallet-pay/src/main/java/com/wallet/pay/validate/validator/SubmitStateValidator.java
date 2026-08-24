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
 * 提交校验：终态失败单（CLOSED/FAIL）不可再提交；SUCCESS/PAYING 的幂等分支由服务层处理。
 */
@Component
public class SubmitStateValidator implements PayValidator {

    @Override
    public Set<PayScene> scenes() {
        return Set.of(PayScene.SUBMIT);
    }

    @Override
    public void validate(PayValidationContext context) {
        OrderState state = context.order().getState();
        if (state == OrderState.CLOSED || state == OrderState.FAIL) {
            throw new BizException(OrderError.ORDER_STATE_INVALID, "state=" + state);
        }
    }

    @Override
    public int order() {
        return 20;
    }
}
