package com.wallet.pay.service;

import com.wallet.pay.entity.PayOrder;
import com.wallet.pay.entity.PayPart;
import com.wallet.pay.enums.PayType;
import com.wallet.pay.event.OrderPaidEvent;
import com.wallet.pay.mapper.PayOrderMapper;
import com.wallet.pay.mapper.PayPartMapper;
import com.wallet.pay.state.OrderState;
import com.wallet.pay.state.PartState;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 结单器：全部分段 SUCCESS 时把主单 CAS 推进 SUCCESS 并发布 {@link OrderPaidEvent}。
 * 纯资产完成、渠道回调监听、补单三条路径共用，保证 markPaid 与事件发布只有这一处。
 * <b>必须在持单锁内调用。</b>
 */
@Slf4j
@Component
@AllArgsConstructor
public class OrderFinisher {

    private final PayOrderMapper payOrderMapper;
    private final PayPartMapper payPartMapper;
    private final ApplicationEventPublisher events;


    /**
     * 全部分段 SUCCESS 才推进主单（可退金额 = 总额 - 券面额）；
     * CAS 影响行数=1 时发布支付成功事件（对同一订单至多一次）。
     */
    public void finishIfAllSuccess(String orderNo) {
        List<PayPart> parts = payPartMapper.findByOrderNo(orderNo);
        long couponAmount = 0;
        for (PayPart part : parts) {
            if (part.getState() != PartState.SUCCESS) {
                log.info("分段未全部成功，等待其余分段, orderNo={}, partNo={}, state={}",
                    orderNo, part.getPartNo(), part.getState());
                return;
            }
            if (part.getPayType() == PayType.COUPON) {
                couponAmount += part.getAmount();
            }
        }
        if (payOrderMapper.markPaid(orderNo, OrderState.PAYING, OrderState.SUCCESS,
            LocalDateTime.now(), couponAmount) == 1) {
            PayOrder order = payOrderMapper.findByOrderNo(orderNo);
            events.publishEvent(new OrderPaidEvent(orderNo, order.getUserId(), order.getTotalAmount()));
            log.info("订单支付完成, orderNo={}", orderNo);
        }
    }
}
