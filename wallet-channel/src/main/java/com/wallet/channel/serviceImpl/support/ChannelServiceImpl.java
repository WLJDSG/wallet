package com.wallet.channel.serviceImpl.support;

import com.wallet.contract.channel.action.Channel;
import com.wallet.channel.core.ChannelTable;
import com.wallet.channel.core.PayFlow;
import com.wallet.contract.channel.ChannelService;
import com.wallet.common.enums.ActionType;
import com.wallet.contract.channel.model.CallbackRequest;
import com.wallet.contract.channel.model.CallLog;
import com.wallet.contract.channel.model.ConfirmRequest;
import com.wallet.contract.channel.model.PayRequest;
import com.wallet.contract.channel.model.PayResult;
import com.wallet.contract.channel.model.QueryRequest;
import com.wallet.contract.channel.model.RefundRequest;
import com.wallet.contract.channel.spi.CallLogWriter;
import com.wallet.contract.channel.spi.FeeRule;
import com.wallet.contract.channel.spi.PayListener;
import com.wallet.contract.channel.spi.RefundStore;
import com.wallet.contract.channel.spi.TradeStore;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 渠道内核组装入口。
 *
 * <p>调用方提供 3 个必选接口实现（交易单存储、退款单存储、支付事件监听）、
 * 2 个可选实现（调用日志出口、手续费规则）与至少一个渠道，构建后通过
 * {@link #flow()} 使用全部编排能力：</p>
 *
 * <pre>{@code
 * ChannelServiceImpl kit = ChannelServiceImpl.builder()
 *     .addChannel(mockPay)              // 一个类可同时实现多个动作接口
 *     .tradeStore(tradeStore)
 *     .refundStore(refundStore)
 *     .listener(payListener)
 *     .logWriter(logWriter)             // 可选，缺省仅打 slf4j 日志
 *     .feeRule(feeRule)                 // 可选，缺省不加手续费
 *     .build();                         // 构建期校验渠道动作完整性，配置错误立即失败
 * kit.flow().pay(payRequest);
 * }</pre>
 *
 * <p>Kit 无状态且线程安全，应用内建一个单例即可。
 * <b>注意：内核不加锁，调用 flow() 各方法前必须持有该支付单的分布式锁。</b></p>
 */
@Slf4j
public final class ChannelServiceImpl implements ChannelService {

    private final ChannelTable table;
    private final PayFlow flow;

    private ChannelServiceImpl(Builder builder) {
        this.table = new ChannelTable(builder.channels);
        this.flow = new PayFlow(table, builder.tradeStore, builder.refundStore, builder.logWriter,
            builder.listener, builder.feeRule);
        log.info("wallet-channel 装配完成, 渠道: {}", table.channelCodes());
    }

    public static Builder builder() {
        return new Builder();
    }

    /** @return 支付编排器（pay/callback/query/refund/cancel/confirm） */
    public PayFlow flow() {
        return flow;
    }

    /** 渠道是否支持某动作（可据此裁剪前端能力，如是否展示"取消支付"入口） */
    @Override
    public boolean supports(String channelCode, ActionType action) {
        return table.supports(channelCode, action);
    }

    /** @return 已注册渠道编码（有序） */
    @Override
    public Set<String> channelCodes() {
        return table.channelCodes();
    }

    @Override
    public PayResult pay(PayRequest request) {
        return flow.pay(request);
    }

    @Override
    public String callback(CallbackRequest request) {
        return flow.callback(request);
    }

    @Override
    public boolean query(QueryRequest request) {
        return flow.query(request);
    }

    @Override
    public boolean refund(RefundRequest request) {
        return flow.refund(request);
    }

    @Override
    public boolean cancel(String channelCode, String orderNo, String outTradeNo) {
        return flow.cancel(channelCode, orderNo, outTradeNo);
    }

    @Override
    public boolean confirm(ConfirmRequest request) {
        return flow.confirm(request);
    }

    public static final class Builder {

        private final List<Channel> channels = new ArrayList<>();
        private TradeStore tradeStore;
        private RefundStore refundStore;
        private PayListener listener;
        private CallLogWriter logWriter = new Slf4jCallLogWriter();
        private FeeRule feeRule = FeeRule.NO_FEE;

        private Builder() {
        }

        public Builder addChannel(Channel channel) {
            this.channels.add(Objects.requireNonNull(channel, "channel 不能为空"));
            return this;
        }

        public Builder channels(Collection<? extends Channel> channels) {
            this.channels.addAll(Objects.requireNonNull(channels, "channels 不能为空"));
            return this;
        }

        public Builder tradeStore(TradeStore tradeStore) {
            this.tradeStore = tradeStore;
            return this;
        }

        public Builder refundStore(RefundStore refundStore) {
            this.refundStore = refundStore;
            return this;
        }

        public Builder listener(PayListener listener) {
            this.listener = listener;
            return this;
        }

        public Builder logWriter(CallLogWriter logWriter) {
            this.logWriter = logWriter;
            return this;
        }

        public Builder feeRule(FeeRule feeRule) {
            this.feeRule = feeRule;
            return this;
        }

        public ChannelServiceImpl build() {
            Objects.requireNonNull(tradeStore, "tradeStore 未实现：请提供 TradeStore");
            Objects.requireNonNull(refundStore, "refundStore 未实现：请提供 RefundStore");
            Objects.requireNonNull(listener, "listener 未实现：请提供 PayListener");
            Objects.requireNonNull(logWriter, "logWriter 不能为 null（可不设置，默认 slf4j 输出）");
            Objects.requireNonNull(feeRule, "feeRule 不能为 null（可不设置，默认不加手续费）");
            return new ChannelServiceImpl(this);
        }
    }

    /** 缺省日志出口：仅打应用日志，生产环境建议实现落库版本 */
    @Slf4j(topic = "wallet.channel.calllog")
    private static final class Slf4jCallLogWriter implements CallLogWriter {

        @Override
        public void write(CallLog record) {
            log.info("channel={}, action={}, orderNo={}, outTradeNo={}, cost={}ms, error={}, req={}, resp={}",
                record.channelCode(), record.action(), record.orderNo(), record.outTradeNo(),
                record.costMs(), record.error(), record.request(), record.response());
        }
    }
}
