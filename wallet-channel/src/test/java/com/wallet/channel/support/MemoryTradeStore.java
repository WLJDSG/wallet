package com.wallet.channel.support;

import com.wallet.contract.channel.enums.PayState;
import com.wallet.contract.channel.model.PayRequest;
import com.wallet.contract.channel.model.TradeInfo;
import com.wallet.contract.channel.spi.TradeStore;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 内存版交易单存储：模拟条件更新（CAS）语义，读取返回不可变快照。
 */
public class MemoryTradeStore implements TradeStore {

    /** 内部可变行，读取时转成不可变的 TradeInfo */
    private static final class Row {
        String orderNo;
        String outTradeNo;
        String channelCode;
        PayState state;
        long amount;
        String currency;
        long refundableAmount;
        String thirdOutTradeNo;
    }

    private final Map<String, Row> rows = new ConcurrentHashMap<>();
    private final AtomicInteger seq = new AtomicInteger();

    @Override
    public TradeInfo create(PayRequest request, long amount) {
        Row row = new Row();
        row.orderNo = request.orderNo();
        row.outTradeNo = request.orderNo() + "-T" + seq.incrementAndGet();
        row.channelCode = request.channelCode();
        row.state = PayState.INIT;
        row.amount = amount;
        row.currency = request.currency();
        row.refundableAmount = amount;
        rows.put(row.outTradeNo, row);
        return toInfo(row);
    }

    @Override
    public TradeInfo find(String channelCode, String orderNo, String outTradeNo) {
        Row row = rows.get(outTradeNo);
        if (row == null || !Objects.equals(row.channelCode, channelCode)
            || !Objects.equals(row.orderNo, orderNo)) {
            return null;
        }
        return toInfo(row);
    }

    @Override
    public synchronized boolean changeState(String outTradeNo, PayState from, PayState to,
        String thirdOutTradeNo) {
        Row row = rows.get(outTradeNo);
        if (row == null || row.state != from) {
            return false;
        }
        row.state = to;
        if (thirdOutTradeNo != null) {
            row.thirdOutTradeNo = thirdOutTradeNo;
        }
        return true;
    }

    @Override
    public synchronized void applyRefund(String outTradeNo, long amount, boolean allRefund) {
        Row row = rows.get(outTradeNo);
        if (row != null) {
            row.refundableAmount = row.refundableAmount - amount;
        }
    }

    public PayState stateOf(String outTradeNo) {
        Row row = rows.get(outTradeNo);
        return row == null ? null : row.state;
    }

    public Long refundableOf(String outTradeNo) {
        Row row = rows.get(outTradeNo);
        return row == null ? null : row.refundableAmount;
    }

    private TradeInfo toInfo(Row row) {
        return TradeInfo.builder()
            .orderNo(row.orderNo).outTradeNo(row.outTradeNo).channelCode(row.channelCode)
            .state(row.state).amount(row.amount).currency(row.currency)
            .refundableAmount(row.refundableAmount).thirdOutTradeNo(row.thirdOutTradeNo)
            .build();
    }
}
