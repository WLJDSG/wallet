package com.zbkj.paychannel;

import com.zbkj.paychannel.core.PaymentOrchestrator;
import com.zbkj.paychannel.core.ProviderRegistry;
import com.zbkj.paychannel.enums.PayActionEnum;
import com.zbkj.paychannel.model.PayLogRecord;
import com.zbkj.paychannel.provider.ChannelProvider;
import com.zbkj.paychannel.spi.FeePolicy;
import com.zbkj.paychannel.spi.PayEventListener;
import com.zbkj.paychannel.spi.PayLockManager;
import com.zbkj.paychannel.spi.PayLogSink;
import com.zbkj.paychannel.spi.PayOrderRepository;
import com.zbkj.paychannel.spi.RefundOrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 多渠道支付编排 SDK 组装入口。
 *
 * <p>宿主提供 4 个必选 SPI（交易单仓储、退款单仓储、分布式锁、支付事件监听）、
 * 2 个可选 SPI（支付日志出口、手续费策略）与至少一个渠道 Provider，构建后通过
 * {@link #payment()} 使用全部编排能力：</p>
 *
 * <pre>{@code
 * PayChannelKernel kernel = PayChannelKernel.builder()
 *     .addProvider(jkoPayProvider)          // 一个类可同时实现多个动作接口
 *     .addProvider(jkoQueryProvider)
 *     .payOrderRepository(payOrderRepository)
 *     .refundOrderRepository(refundOrderRepository)
 *     .lockManager(lockManager)
 *     .eventListener(eventListener)
 *     .logSink(logSink)                     // 可选，缺省仅打 slf4j 日志
 *     .feePolicy(feePolicy)                 // 可选，缺省不加手续费
 *     .build();                             // 构建期校验渠道动作完整性，配置错误立即失败
 * kernel.payment().pay(payCommand);
 * }</pre>
 *
 * <p>Kernel 无状态且线程安全，宿主应用内建一个单例即可。</p>
 */
public final class PayChannelKernel {

    private static final Logger log = LoggerFactory.getLogger(PayChannelKernel.class);

    private final ProviderRegistry registry;
    private final PaymentOrchestrator orchestrator;

    private PayChannelKernel(Builder builder) {
        this.registry = new ProviderRegistry(builder.providers);
        this.orchestrator = new PaymentOrchestrator(registry, builder.payOrderRepository,
            builder.refundOrderRepository, builder.lockManager, builder.logSink, builder.eventListener,
            builder.feePolicy);
        log.info("pay-channel-sdk 装配完成, 渠道: {}", registry.channelCodes());
    }

    public static Builder builder() {
        return new Builder();
    }

    /** @return 支付编排器（pay/handleCallback/queryPayResult/refund/cancel/executePayment） */
    public PaymentOrchestrator payment() {
        return orchestrator;
    }

    /** 渠道是否支持某动作（宿主可据此裁剪前端能力，如是否展示"取消支付"入口） */
    public boolean supports(String channelCode, PayActionEnum action) {
        return registry.supports(channelCode, action);
    }

    /** @return 已注册渠道编码（有序） */
    public Set<String> channelCodes() {
        return registry.channelCodes();
    }

    public static final class Builder {

        private final List<ChannelProvider> providers = new ArrayList<>();
        private PayOrderRepository payOrderRepository;
        private RefundOrderRepository refundOrderRepository;
        private PayLockManager lockManager;
        private PayEventListener eventListener;
        private PayLogSink logSink = new Slf4jPayLogSink();
        private FeePolicy feePolicy = FeePolicy.NO_FEE;

        private Builder() {
        }

        public Builder addProvider(ChannelProvider provider) {
            this.providers.add(Objects.requireNonNull(provider, "provider 不能为空"));
            return this;
        }

        public Builder providers(Collection<? extends ChannelProvider> providers) {
            this.providers.addAll(Objects.requireNonNull(providers, "providers 不能为空"));
            return this;
        }

        public Builder payOrderRepository(PayOrderRepository payOrderRepository) {
            this.payOrderRepository = payOrderRepository;
            return this;
        }

        public Builder refundOrderRepository(RefundOrderRepository refundOrderRepository) {
            this.refundOrderRepository = refundOrderRepository;
            return this;
        }

        public Builder lockManager(PayLockManager lockManager) {
            this.lockManager = lockManager;
            return this;
        }

        public Builder eventListener(PayEventListener eventListener) {
            this.eventListener = eventListener;
            return this;
        }

        public Builder logSink(PayLogSink logSink) {
            this.logSink = logSink;
            return this;
        }

        public Builder feePolicy(FeePolicy feePolicy) {
            this.feePolicy = feePolicy;
            return this;
        }

        public PayChannelKernel build() {
            Objects.requireNonNull(payOrderRepository, "payOrderRepository 未实现：请提供 PayOrderRepository");
            Objects.requireNonNull(refundOrderRepository,
                "refundOrderRepository 未实现：请提供 RefundOrderRepository");
            Objects.requireNonNull(lockManager, "lockManager 未实现：请提供 PayLockManager");
            Objects.requireNonNull(eventListener, "eventListener 未实现：请提供 PayEventListener");
            Objects.requireNonNull(logSink, "logSink 不能为 null（可不设置，默认 slf4j 输出）");
            Objects.requireNonNull(feePolicy, "feePolicy 不能为 null（可不设置，默认不加手续费）");
            return new PayChannelKernel(this);
        }
    }

    /** 缺省日志出口：仅打应用日志，生产环境建议宿主实现落库版本 */
    private static final class Slf4jPayLogSink implements PayLogSink {

        private static final Logger payLog = LoggerFactory.getLogger("paychannel.paylog");

        @Override
        public void record(PayLogRecord record) {
            payLog.info("channel={}, action={}, orderNo={}, outTradeNo={}, cost={}ms, error={}, req={}, resp={}",
                record.getChannelCode(), record.getAction(), record.getOrderNo(), record.getOutTradeNo(),
                record.getCostMillis(), record.getErrorMessage(), record.getRequestJson(),
                record.getResponseJson());
        }
    }
}
