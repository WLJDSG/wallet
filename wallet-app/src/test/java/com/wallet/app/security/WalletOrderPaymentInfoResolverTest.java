package com.wallet.app.security;

import com.wallet.common.enums.OrderState;
import com.wallet.common.enums.PayType;
import com.wallet.pay.entity.PayOrder;
import com.wallet.pay.entity.PayPart;
import com.wallet.pay.mapper.PayOrderMapper;
import com.wallet.pay.mapper.PayPartMapper;
import com.wallet.security.model.OrderPaymentInfo;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WalletOrderPaymentInfoResolverTest {

    private final PayOrderMapper orders = mock(PayOrderMapper.class);
    private final PayPartMapper parts = mock(PayPartMapper.class);
    private final WalletOrderPaymentInfoResolver resolver = new WalletOrderPaymentInfoResolver(orders, parts);

    @Test
    void resolvesOwnedOrderAndSumsMoneyPartsOnly() {
        PayOrder order = new PayOrder();
        order.setUserId(7L);
        order.setCurrency("CNY");
        order.setState(OrderState.INIT);
        when(orders.findByOrderNo("P1")).thenReturn(order);
        when(parts.findByOrderNo("P1")).thenReturn(List.of(
            part(PayType.MONEY, 700L), part(PayType.MONEY, 500L), part(PayType.COUPON, 300L)));

        OrderPaymentInfo result = resolver.resolve(7L, "P1", "WALLET");

        assertEquals(new BigDecimal("1200"), result.getAmount());
        assertEquals("CNY", result.getCurrency());
        assertFalse(result.getPaid());
    }

    @Test
    void hidesOrderOwnedByAnotherUser() {
        PayOrder order = new PayOrder();
        order.setUserId(8L);
        when(orders.findByOrderNo("P2")).thenReturn(order);

        assertNull(resolver.resolve(7L, "P2", "WALLET"));
    }

    private PayPart part(PayType type, long amount) {
        PayPart part = new PayPart();
        part.setPayType(type);
        part.setAmount(amount);
        return part;
    }
}
