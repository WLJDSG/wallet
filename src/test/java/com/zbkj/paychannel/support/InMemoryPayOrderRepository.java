package com.zbkj.paychannel.support;

import com.zbkj.paychannel.enums.PayStateEnum;
import com.zbkj.paychannel.model.PayCommand;
import com.zbkj.paychannel.model.PayOrderSnapshot;
import com.zbkj.paychannel.spi.PayOrderRepository;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 内存版交易单仓储：模拟条件更新（CAS）语义，返回副本模拟数据库快照读。
 */
public class InMemoryPayOrderRepository implements PayOrderRepository {

    private final Map<String, PayOrderSnapshot> store = new ConcurrentHashMap<>();
    private final AtomicInteger seq = new AtomicInteger();

    @Override
    public PayOrderSnapshot create(PayCommand command, BigDecimal payAmount) {
        String outTradeNo = command.getOrderNo() + "-T" + seq.incrementAndGet();
        PayOrderSnapshot snapshot = PayOrderSnapshot.builder()
            .orderNo(command.getOrderNo())
            .outTradeNo(outTradeNo)
            .channelCode(command.getChannelCode())
            .state(PayStateEnum.INIT)
            .payAmount(payAmount)
            .currency(command.getCurrency())
            .refundableAmount(payAmount)
            .build();
        store.put(outTradeNo, snapshot);
        return copy(snapshot);
    }

    @Override
    public PayOrderSnapshot find(String channelCode, String orderNo, String outTradeNo) {
        PayOrderSnapshot snapshot = store.get(outTradeNo);
        if (snapshot == null || !Objects.equals(snapshot.getChannelCode(), channelCode)
            || !Objects.equals(snapshot.getOrderNo(), orderNo)) {
            return null;
        }
        return copy(snapshot);
    }

    @Override
    public synchronized boolean transitionState(String outTradeNo, PayStateEnum from, PayStateEnum to,
        String thirdOutTradeNo) {
        PayOrderSnapshot snapshot = store.get(outTradeNo);
        if (snapshot == null || snapshot.getState() != from) {
            return false;
        }
        snapshot.setState(to);
        if (thirdOutTradeNo != null) {
            snapshot.setThirdOutTradeNo(thirdOutTradeNo);
        }
        return true;
    }

    @Override
    public synchronized void applyRefund(String outTradeNo, BigDecimal refundAmount, boolean allRefund) {
        PayOrderSnapshot snapshot = store.get(outTradeNo);
        if (snapshot != null) {
            snapshot.setRefundableAmount(snapshot.getRefundableAmount().subtract(refundAmount));
        }
    }

    public PayStateEnum stateOf(String outTradeNo) {
        PayOrderSnapshot snapshot = store.get(outTradeNo);
        return snapshot == null ? null : snapshot.getState();
    }

    public BigDecimal refundableOf(String outTradeNo) {
        PayOrderSnapshot snapshot = store.get(outTradeNo);
        return snapshot == null ? null : snapshot.getRefundableAmount();
    }

    private PayOrderSnapshot copy(PayOrderSnapshot s) {
        return PayOrderSnapshot.builder().orderNo(s.getOrderNo()).outTradeNo(s.getOutTradeNo())
            .channelCode(s.getChannelCode()).state(s.getState()).payAmount(s.getPayAmount())
            .currency(s.getCurrency()).refundableAmount(s.getRefundableAmount())
            .thirdOutTradeNo(s.getThirdOutTradeNo()).build();
    }
}
