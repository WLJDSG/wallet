package com.wallet.app.security;

import com.wallet.common.enums.OrderState;
import com.wallet.common.enums.PayType;
import com.wallet.pay.entity.PayOrder;
import com.wallet.pay.mapper.PayOrderMapper;
import com.wallet.pay.mapper.PayPartMapper;
import com.wallet.security.error.PaySecurityErrorCode;
import com.wallet.security.error.PaySecurityException;
import com.wallet.security.model.OrderPaymentInfo;
import com.wallet.security.spi.OrderPaymentInfoResolver;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** 从钱包支付单实时解析本次余额段金额；客户端金额不参与安全决策。 */
@Component
public class WalletOrderPaymentInfoResolver implements OrderPaymentInfoResolver {

    private final PayOrderMapper orders;
    private final PayPartMapper parts;

    public WalletOrderPaymentInfoResolver(PayOrderMapper orders, PayPartMapper parts) {
        this.orders = orders;
        this.parts = parts;
    }

    @Override
    public OrderPaymentInfo resolve(Long uid, String orderNo, String orderType) {
        if (!"WALLET".equals(orderType)) {
            throw PaySecurityException.of(PaySecurityErrorCode.PAY_SECURITY_ORDER_TYPE_UNSUPPORTED);
        }
        PayOrder order = orders.findByOrderNo(orderNo);
        if (order == null || !uid.equals(order.getUserId())) {
            return null;
        }
        long moneyAmount = parts.findByOrderNo(orderNo).stream()
            .filter(part -> part.getPayType() == PayType.MONEY)
            .mapToLong(part -> part.getAmount()).sum();
        return new OrderPaymentInfo(orderNo, orderType, BigDecimal.valueOf(moneyAmount), order.getCurrency(),
            order.getState() == OrderState.SUCCESS);
    }
}
