package com.wallet.pay.validate.validator;

import com.wallet.asset.service.password.PasswordService;
import com.wallet.common.error.BizException;
import com.wallet.pay.entity.PayPart;
import com.wallet.pay.error.OrderError;
import com.wallet.pay.state.OrderState;
import com.wallet.pay.validate.PayScene;
import com.wallet.pay.validate.PayValidationContext;
import com.wallet.pay.validate.PayValidator;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 提交校验：含资产段的首次提交（主单 INIT）必须携带支付密码票据，
 * 校验通过即原子消费票据并复核用户/订单/金额三者一致（链上最后一个执行——有副作用）。
 * 重复提交（主单已 PAYING）不再要求票据。
 */
@Component
@AllArgsConstructor
public class SubmitTicketValidator implements PayValidator {

    private final PasswordService passwordService;

    @Override
    public Set<PayScene> scenes() {
        return Set.of(PayScene.SUBMIT);
    }

    @Override
    public void validate(PayValidationContext context) {
        if (context.order().getState() != OrderState.INIT || !hasAssetPart(context)) {
            return;
        }
        if (context.ticket() == null || context.ticket().trim().isEmpty()) {
            throw new BizException(OrderError.TICKET_REQUIRED, context.orderNo());
        }
        passwordService.consumeTicket(context.ticket(), context.userId(), context.orderNo(),
            context.order().getTotalAmount());
    }

    private boolean hasAssetPart(PayValidationContext context) {
        for (PayPart part : context.parts()) {
            if (part.getPayType().isAsset()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int order() {
        return 30;
    }
}
