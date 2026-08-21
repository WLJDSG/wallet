package com.wallet.pay.service;

import com.wallet.channel.ChannelKit;
import com.wallet.channel.model.CallbackRequest;
import com.wallet.common.lock.LockService;
import com.wallet.pay.config.MockProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * mock 渠道自动回调：下单后 N 秒模拟渠道推送支付结果。
 * 直接走回调处理链路（验签→分段成功→主单成功），便于联调验证。
 * notifySeconds <= 0 时不自动，改用手工调用回调接口。
 */
@Component
public class NotifyService {

    private static final Logger log = LoggerFactory.getLogger(NotifyService.class);

    private static final ScheduledExecutorService SCHEDULER =
        Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "mock-notify");
            thread.setDaemon(true);
            return thread;
        });

    private final MockProperties props;
    private final ChannelKit channelKit;
    private final LockService lockService;

    public NotifyService(MockProperties props, ChannelKit channelKit, LockService lockService) {
        this.props = props;
        this.channelKit = channelKit;
        this.lockService = lockService;
    }

    public void scheduleAutoNotify(String orderNo, String partNo) {
        if (props.getNotifySeconds() <= 0) {
            return;
        }
        SCHEDULER.schedule(() -> notifyNow(orderNo, partNo), props.getNotifySeconds(), TimeUnit.SECONDS);
    }

    public void notifyNow(String orderNo, String partNo) {
        try {
            lockService.withLock(LockService.payOrderKey(orderNo), () -> {
                CallbackRequest request = CallbackRequest.builder()
                    .channelCode("MOCK")
                    .orderNo(orderNo)
                    .outTradeNo(partNo)
                    .httpMethod("POST")
                    .requestUri("/api/pay/callback/MOCK/" + orderNo + "/" + partNo)
                    .headers(Map.of("x-mock-token", props.getSecret()))
                    .body("{\"result\":\"SUCCESS\"}")
                    .build();
                channelKit.flow().callback(request);
                return null;
            });
        } catch (Exception e) {
            log.warn("mock 自动回调处理失败, orderNo={}, partNo={}, err={}", orderNo, partNo, e.getMessage());
        }
    }
}
