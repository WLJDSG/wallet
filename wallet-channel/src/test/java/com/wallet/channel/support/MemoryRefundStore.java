package com.wallet.channel.support;

import com.wallet.channel.enums.RefundState;
import com.wallet.channel.model.RefundInfo;
import com.wallet.channel.spi.RefundStore;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存版退款单存储。
 */
public class MemoryRefundStore implements RefundStore {

    private static final class Row {
        String refundOrderNo;
        String outTradeNo;
        RefundState state;
        long amount;
        String currency;
    }

    private final Map<String, Row> rows = new ConcurrentHashMap<>();

    @Override
    public RefundInfo create(String outTradeNo, String refundOrderNo, long amount, String currency) {
        Row row = new Row();
        row.refundOrderNo = refundOrderNo;
        row.outTradeNo = outTradeNo;
        row.state = RefundState.INIT;
        row.amount = amount;
        row.currency = currency;
        rows.put(refundOrderNo, row);
        return new RefundInfo(refundOrderNo, outTradeNo, row.state, amount, currency);
    }

    @Override
    public synchronized boolean changeState(String refundOrderNo, RefundState from, RefundState to) {
        Row row = rows.get(refundOrderNo);
        if (row == null || row.state != from) {
            return false;
        }
        row.state = to;
        return true;
    }

    public RefundState stateOf(String refundOrderNo) {
        Row row = rows.get(refundOrderNo);
        return row == null ? null : row.state;
    }

    public Long amountOf(String refundOrderNo) {
        Row row = rows.get(refundOrderNo);
        return row == null ? null : row.amount;
    }
}
