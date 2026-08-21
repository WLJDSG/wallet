package com.wallet.pay.adapter;

import com.wallet.channel.spi.PayListener;
import com.wallet.pay.mapper.PayOrderMapper;
import com.wallet.pay.mapper.PayPartMapper;
import com.wallet.pay.entity.PayPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 渠道内核事件监听适配。
 *
 * <p>onPaySuccess：渠道分段已被内核 CAS 推进为 SUCCESS，这里检查全部分段，
 * 全部成功后把主单推进 SUCCESS（内层锁已由调用方持有，无并发窗口）。</p>
 */
@Component
public class PayListenerImpl implements PayListener {

    private static final Logger log = LoggerFactory.getLogger(PayListenerImpl.class);

    private final PayOrderMapper payOrderMapper;
    private final PayPartMapper payPartMapper;

    public PayListenerImpl(PayOrderMapper payOrderMapper, PayPartMapper payPartMapper) {
        this.payOrderMapper = payOrderMapper;
        this.payPartMapper = payPartMapper;
    }

    @Override
    public void onPaySuccess(String channelCode, String orderNo, String outTradeNo) {
        List<PayPart> parts = payPartMapper.findByOrderNo(orderNo);
        boolean allSuccess = true;
        long couponAmount = 0;
        for (PayPart part : parts) {
            if (!"SUCCESS".equals(part.getState())) {
                allSuccess = false;
            }
            if ("COUPON".equals(part.getPayType())) {
                couponAmount += part.getAmount();
            }
        }
        if (allSuccess) {
            payOrderMapper.markPaid(orderNo, "PAYING", "SUCCESS", LocalDateTime.now(), couponAmount);
            log.info("订单支付完成, orderNo={}", orderNo);
        } else {
            log.info("渠道分段支付成功，等待其余分段, orderNo={}, partNo={}", orderNo, outTradeNo);
        }
    }

    @Override
    public void onRefundSuccess(String channelCode, String orderNo, String refundOrderNo, long amount) {
        // 三方退款分段由内核推进 SUCCESS；资产分段返还在 RefundService 内继续处理，这里仅记录
        log.info("渠道退款成功, orderNo={}, refundPartNo={}, amount={}", orderNo, refundOrderNo, amount);
    }
}
