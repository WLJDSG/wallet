package com.wallet.pay.adapter;

import com.wallet.channel.enums.PayError;
import com.wallet.channel.enums.RefundState;
import com.wallet.channel.error.ChannelException;
import com.wallet.channel.model.RefundInfo;
import com.wallet.channel.spi.RefundStore;
import com.wallet.pay.entity.RefundPart;
import com.wallet.pay.mapper.RefundPartMapper;
import org.springframework.stereotype.Component;

/**
 * 渠道内核的退款单存储适配：映射到 refund_part 表的"三方退款分段"。
 * 三方退款分段在分摊时已预建，refund_part_no 即内核的退款单号。
 */
@Component
public class RefundStoreImpl implements RefundStore {

    private final RefundPartMapper refundPartMapper;

    public RefundStoreImpl(RefundPartMapper refundPartMapper) {
        this.refundPartMapper = refundPartMapper;
    }

    @Override
    public RefundInfo create(String outTradeNo, String refundOrderNo, long amount, String currency) {
        RefundPart part = refundPartMapper.findByRefundPartNo(refundOrderNo);
        if (part == null) {
            throw new ChannelException(PayError.ORDER_DOES_NOT_EXIST, "refundPartNo=" + refundOrderNo);
        }
        return new RefundInfo(refundOrderNo, outTradeNo, part.getState(), amount, currency);
    }

    @Override
    public boolean changeState(String refundOrderNo, RefundState from, RefundState to) {
        return refundPartMapper.changeState(refundOrderNo, from, to) == 1;
    }
}
