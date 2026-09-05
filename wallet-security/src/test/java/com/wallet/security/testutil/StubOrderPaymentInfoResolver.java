package com.wallet.security.testutil;

import com.wallet.security.error.PaySecurityErrorCode;
import com.wallet.security.error.PaySecurityException;
import com.wallet.security.model.OrderPaymentInfo;
import com.wallet.security.spi.OrderPaymentInfoResolver;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 按订单号登记订单支付信息的解析测试替身；未登记返回 null；
 * orderType 为 UNSUPPORTED 时模拟宿主抛不支持。
 */
public class StubOrderPaymentInfoResolver implements OrderPaymentInfoResolver {

    private final Map<String, OrderPaymentInfo> orders = new HashMap<>();

    public void register(String orderNo, String orderType, BigDecimal amount, String currency, boolean paid) {
        orders.put(orderNo, new OrderPaymentInfo(orderNo, orderType, amount, currency, paid));
    }

    public void markPaid(String orderNo) {
        OrderPaymentInfo info = orders.get(orderNo);
        info.setPaid(true);
    }

    public void changeAmount(String orderNo, BigDecimal amount) {
        orders.get(orderNo).setAmount(amount);
    }

    @Override
    public OrderPaymentInfo resolve(Long uid, String orderNo, String orderType) {
        if ("UNSUPPORTED".equals(orderType)) {
            throw PaySecurityException.of(PaySecurityErrorCode.PAY_SECURITY_ORDER_TYPE_UNSUPPORTED);
        }
        return orders.get(orderNo);
    }
}
