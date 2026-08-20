package com.zbkj.paychannel.support;

import com.zbkj.paychannel.enums.RefundStateEnum;
import com.zbkj.paychannel.model.RefundOrderSnapshot;
import com.zbkj.paychannel.spi.RefundOrderRepository;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存版退款单仓储。
 */
public class InMemoryRefundOrderRepository implements RefundOrderRepository {

    private final Map<String, RefundOrderSnapshot> store = new ConcurrentHashMap<>();

    @Override
    public RefundOrderSnapshot create(String outTradeNo, String refundOrderNo, BigDecimal refundAmount,
        String currency) {
        RefundOrderSnapshot snapshot = RefundOrderSnapshot.builder()
            .refundOrderNo(refundOrderNo)
            .outTradeNo(outTradeNo)
            .state(RefundStateEnum.INIT)
            .refundAmount(refundAmount)
            .currency(currency)
            .build();
        store.put(refundOrderNo, snapshot);
        return snapshot;
    }

    @Override
    public synchronized boolean transitionState(String refundOrderNo, RefundStateEnum from, RefundStateEnum to) {
        RefundOrderSnapshot snapshot = store.get(refundOrderNo);
        if (snapshot == null || snapshot.getState() != from) {
            return false;
        }
        snapshot.setState(to);
        return true;
    }

    public RefundStateEnum stateOf(String refundOrderNo) {
        RefundOrderSnapshot snapshot = store.get(refundOrderNo);
        return snapshot == null ? null : snapshot.getState();
    }

    public BigDecimal amountOf(String refundOrderNo) {
        RefundOrderSnapshot snapshot = store.get(refundOrderNo);
        return snapshot == null ? null : snapshot.getRefundAmount();
    }
}
