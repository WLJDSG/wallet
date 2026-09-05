package com.wallet.pay.validate.validator;

import com.wallet.common.error.CommonException;
import com.wallet.security.PaySecurityEngine;
import com.wallet.security.model.AuthorizationConsumeCommand;
import com.wallet.security.model.UserIdentity;
import com.wallet.pay.entity.PayPart;
import com.wallet.common.error.ErrorCode;
import com.wallet.common.enums.OrderState;
import com.wallet.pay.validate.PayScene;
import com.wallet.pay.validate.PayValidationContext;
import com.wallet.pay.validate.PayValidator;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.math.BigDecimal;

/**
 * 提交校验：含余额支付段的首次提交（主单 INIT）必须携带支付授权票据，
 * 校验通过即原子消费票据并复核用户/订单/金额三者一致（链上最后一个执行——有副作用）。
 * 重复提交（主单已 PAYING）不再要求票据。
 */
@Component
@AllArgsConstructor
public class SubmitTicketValidator implements PayValidator {

    private final PaySecurityEngine paySecurityEngine;

    @Override
    public Set<PayScene> scenes() {
        return Set.of(PayScene.SUBMIT);
    }

    @Override
    public void validate(PayValidationContext context) {
        long moneyAmount = moneyAmount(context);
        if (context.order().getState() != OrderState.INIT || moneyAmount <= 0) {
            return;
        }
        if (context.ticket() == null || context.ticket().trim().isEmpty()) {
            throw new CommonException(ErrorCode.TICKET_REQUIRED, context.orderNo());
        }
        AuthorizationConsumeCommand command = new AuthorizationConsumeCommand();
        command.setRequired(true);
        command.setPayAuthorizationToken(context.ticket());
        command.setUser(UserIdentity.of(context.userId(), null, true));
        command.setOrderNo(context.orderNo());
        command.setOrderType("WALLET");
        command.setPayPrice(BigDecimal.valueOf(moneyAmount));
        command.setCurrency(context.order().getCurrency());
        // 唯一消费点位于订单锁内、任何余额扣减之前；失败不会推进订单或扣资产。
        paySecurityEngine.getAuthorizationService().consume(command);
    }

    private long moneyAmount(PayValidationContext context) {
        return context.parts().stream()
            .filter(part -> part.getPayType() == com.wallet.common.enums.PayType.MONEY)
            .mapToLong(PayPart::getAmount)
            .sum();
    }

    @Override
    public int order() {
        return 30;
    }
}
