package com.wallet.pay.adapter;

import lombok.AllArgsConstructor;
import com.wallet.contract.channel.enums.PayError;
import com.wallet.contract.channel.enums.PayState;
import com.wallet.contract.channel.error.ChannelException;
import com.wallet.contract.channel.model.PayRequest;
import com.wallet.contract.channel.model.TradeInfo;
import com.wallet.contract.channel.spi.TradeStore;
import com.wallet.pay.entity.PayPart;
import com.wallet.contract.pay.enums.PayType;
import com.wallet.pay.state.PartState;
import com.wallet.pay.mapper.PayPartMapper;
import org.springframework.stereotype.Component;

/**
 * 渠道内核的交易单存储适配：映射到 pay_part 表的三方分段。
 * 一个支付单至多一个 CHANNEL 分段，part_no 即渠道交易号 outTradeNo。
 */
@Component
@AllArgsConstructor
public class TradeStoreImpl implements TradeStore {

    private final PayPartMapper payPartMapper;


    /** 三方分段在创建支付单时已预建，这里直接返回既有分段 */
    @Override
    public TradeInfo create(PayRequest request, long amount) {
        PayPart part = findChannelPart(request.orderNo());
        if (part == null) {
            throw new ChannelException(PayError.ORDER_DOES_NOT_EXIST,
                "orderNo=" + request.orderNo() + " 没有 CHANNEL 分段");
        }
        return toInfo(part);
    }

    @Override
    public TradeInfo find(String channelCode, String orderNo, String outTradeNo) {
        PayPart part = payPartMapper.findByPartNo(outTradeNo);
        if (part == null || !part.getOrderNo().equals(orderNo) || part.getPayType() != PayType.CHANNEL
            || !part.getChannelCode().equals(channelCode)) {
            return null;
        }
        return toInfo(part);
    }

    @Override
    public boolean changeState(String outTradeNo, PayState from, PayState to, String thirdOutTradeNo) {
        // 内核 PayState 与本地 PartState 同名（PartState 多一个 ROLLBACK，属本地补偿语义）
        PartState localFrom = PartState.valueOf(from.name());
        PartState localTo = PartState.valueOf(to.name());
        if (thirdOutTradeNo != null) {
            return payPartMapper.changeStateWithThird(outTradeNo, localFrom, localTo, thirdOutTradeNo) == 1;
        }
        return payPartMapper.changeState(outTradeNo, localFrom, localTo) == 1;
    }

    @Override
    public void applyRefund(String outTradeNo, long amount, boolean allRefund) {
        payPartMapper.increaseRefunded(outTradeNo, amount);
    }

    private PayPart findChannelPart(String orderNo) {
        for (PayPart part : payPartMapper.findByOrderNo(orderNo)) {
            if (part.getPayType() == PayType.CHANNEL) {
                return part;
            }
        }
        return null;
    }

    private TradeInfo toInfo(PayPart part) {
        return TradeInfo.builder()
            .orderNo(part.getOrderNo())
            .outTradeNo(part.getPartNo())
            .channelCode(part.getChannelCode())
            .state(PayState.valueOf(part.getState().name()))
            .amount(part.getAmount())
            .currency("TWD")
            .refundableAmount(part.getAmount() - part.getRefundedAmount())
            .thirdOutTradeNo(part.getThirdNo())
            .build();
    }
}
