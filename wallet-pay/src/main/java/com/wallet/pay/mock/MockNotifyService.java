package com.wallet.pay.mock;

import com.wallet.common.trace.TraceIds;
import com.wallet.pay.config.MockProperties;
import com.wallet.pay.service.PayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * mock 渠道自动回调：下单后 N 秒模拟渠道推送支付结果。
 * 复用 {@link PayService#handleCallback}（@Lock4j 持单锁）走完整回调链路
 * （验签→分段成功→主单成功），便于联调验证。
 * notifySeconds <= 0 时不自动，改用手工调用回调接口。
 */
@Slf4j
@Component
public class MockNotifyService {

    private static final ScheduledExecutorService SCHEDULER =
        Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "mock-notify");
            thread.setDaemon(true);
            return thread;
        });

    private final MockProperties props;
    // PayService 依赖本类（下单后注册自动回调），用 ObjectProvider 延迟解析避免循环依赖
    private final ObjectProvider<PayService> payService;

    public MockNotifyService(MockProperties props, ObjectProvider<PayService> payService) {
        this.props = props;
        this.payService = payService;
    }

    public void scheduleAutoNotify(String orderNo, String partNo) {
        if (props.getNotifySeconds() <= 0) {
            return;
        }
        SCHEDULER.schedule(() -> notifyNow(orderNo, partNo), props.getNotifySeconds(), TimeUnit.SECONDS);
    }

    public void notifyNow(String orderNo, String partNo) {
        TraceIds.seed();
        try {
            payService.getObject().handleCallback("MOCK", orderNo, partNo, "{\"result\":\"SUCCESS\"}",
                Map.of("x-mock-token", props.getSecret()), "POST",
                "/api/pay/callback/MOCK/" + orderNo + "/" + partNo);
        } catch (Exception e) {
            log.warn("mock 自动回调处理失败, orderNo={}, partNo={}, err={}", orderNo, partNo, e.getMessage());
        } finally {
            TraceIds.clear();
        }
    }
}
