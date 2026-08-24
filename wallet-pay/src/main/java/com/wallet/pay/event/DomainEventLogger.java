package com.wallet.pay.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 领域事件审计日志（也是订阅示例）：业务方需要联动时新写 @EventListener 订阅，
 * 不要在服务之间直接互相调用。
 */
@Slf4j
@Component
public class DomainEventLogger {

    @EventListener
    public void onOrderPaid(OrderPaidEvent event) {
        log.info("[领域事件] 支付成功, orderNo={}, userId={}, totalAmount={}",
            event.orderNo(), event.userId(), event.totalAmount());
    }

    @EventListener
    public void onOrderClosed(OrderClosedEvent event) {
        log.info("[领域事件] 订单关闭, orderNo={}, userId={}", event.orderNo(), event.userId());
    }

    @EventListener
    public void onRefundSuccess(RefundSuccessEvent event) {
        log.info("[领域事件] 退款成功, refundNo={}, orderNo={}, userId={}, amount={}",
            event.refundNo(), event.orderNo(), event.userId(), event.amount());
    }
}
