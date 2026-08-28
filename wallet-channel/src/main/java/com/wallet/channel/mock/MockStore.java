package com.wallet.channel.mock;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * mock 渠道的"渠道侧"交易状态（内存）。真实渠道不存在本类。
 */
@Component
public class MockStore {

    private final Map<String, String> states = new ConcurrentHashMap<>();

    public void markCreated(String outTradeNo) {
        states.put(outTradeNo, "CREATED");
    }

    public void markPaid(String outTradeNo) {
        states.put(outTradeNo, "PAID");
    }

    public void markCancelled(String outTradeNo) {
        states.putIfAbsent(outTradeNo, "CANCELLED");
    }

    public void markRefunded(String outTradeNo) {
        states.put(outTradeNo, "REFUNDED");
    }

    public boolean isPaid(String outTradeNo) {
        return "PAID".equals(states.get(outTradeNo));
    }
}
