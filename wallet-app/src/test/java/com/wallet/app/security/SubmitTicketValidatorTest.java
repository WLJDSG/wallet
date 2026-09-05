package com.wallet.app.security;

import com.wallet.common.enums.OrderState;
import com.wallet.common.enums.PayType;
import com.wallet.pay.entity.PayOrder;
import com.wallet.pay.entity.PayPart;
import com.wallet.pay.validate.PayValidationContext;
import com.wallet.pay.validate.validator.SubmitTicketValidator;
import com.wallet.security.PaySecurityEngine;
import com.wallet.security.model.AuthorizationConsumeCommand;
import com.wallet.security.service.AuthorizationService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SubmitTicketValidatorTest {

    @Test
    void consumesTicketWithServerSideMoneyPartOnly() {
        PaySecurityEngine engine = mock(PaySecurityEngine.class);
        AuthorizationService authorizationService = mock(AuthorizationService.class);
        when(engine.getAuthorizationService()).thenReturn(authorizationService);
        SubmitTicketValidator validator = new SubmitTicketValidator(engine);

        PayOrder order = order(OrderState.INIT);
        validator.validate(PayValidationContext.forSubmit(7L, "P202609040001", order,
            List.of(part(PayType.MONEY, 1200L), part(PayType.COUPON, 300L), part(PayType.POINT, 200L)),
            "authorization-token"));

        ArgumentCaptor<AuthorizationConsumeCommand> captor = ArgumentCaptor.forClass(AuthorizationConsumeCommand.class);
        verify(authorizationService).consume(captor.capture());
        AuthorizationConsumeCommand command = captor.getValue();
        assertEquals(new BigDecimal("1200"), command.getPayPrice());
        assertEquals("P202609040001", command.getOrderNo());
        assertEquals("WALLET", command.getOrderType());
        assertEquals(7L, command.getUser().getUid());
        assertEquals("CNY", command.getCurrency());
    }

    @Test
    void doesNotRequireTicketWhenOrderHasNoMoneyPart() {
        PaySecurityEngine engine = mock(PaySecurityEngine.class);
        AuthorizationService authorizationService = mock(AuthorizationService.class);
        when(engine.getAuthorizationService()).thenReturn(authorizationService);
        SubmitTicketValidator validator = new SubmitTicketValidator(engine);

        validator.validate(PayValidationContext.forSubmit(7L, "P202609040002", order(OrderState.INIT),
            List.of(part(PayType.COUPON, 300L), part(PayType.POINT, 200L)), null));

        verify(authorizationService, never()).consume(org.mockito.ArgumentMatchers.any());
    }

    private PayOrder order(OrderState state) {
        PayOrder order = new PayOrder();
        order.setState(state);
        order.setCurrency("CNY");
        return order;
    }

    private PayPart part(PayType type, long amount) {
        PayPart part = new PayPart();
        part.setPayType(type);
        part.setAmount(amount);
        return part;
    }
}
