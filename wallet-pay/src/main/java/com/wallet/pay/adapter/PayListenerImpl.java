package com.wallet.pay.adapter;

import com.wallet.contract.channel.spi.PayListener;
import com.wallet.pay.service.OrderFinisher;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 渠道内核事件监听适配。
 *
 * <p>onPaySuccess：渠道分段已被内核 CAS 推进为 SUCCESS，交给 {@link OrderFinisher}
 * 检查全部分段并结单（内层锁已由调用方持有，无并发窗口）。</p>
 */
@Slf4j
@Component
@AllArgsConstructor
public class PayListenerImpl implements PayListener {

    private final OrderFinisher orderFinisher;


    @Override
    public void onPaySuccess(String channelCode, String orderNo, String outTradeNo) {
        orderFinisher.finishIfAllSuccess(orderNo);
    }

    @Override
    public void onRefundSuccess(String channelCode, String orderNo, String refundOrderNo, long amount) {
        // 三方退款分段由内核推进 SUCCESS；资产分段返还在 RefundService 内继续处理，这里仅记录
        log.info("渠道退款成功, orderNo={}, refundPartNo={}, amount={}", orderNo, refundOrderNo, amount);
    }
}
