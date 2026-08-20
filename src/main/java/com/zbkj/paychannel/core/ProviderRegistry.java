package com.zbkj.paychannel.core;

import com.zbkj.paychannel.enums.PayActionEnum;
import com.zbkj.paychannel.enums.PayErrorCode;
import com.zbkj.paychannel.exception.PayChannelException;
import com.zbkj.paychannel.provider.CallbackProvider;
import com.zbkj.paychannel.provider.CancelProvider;
import com.zbkj.paychannel.provider.ChannelProvider;
import com.zbkj.paychannel.provider.ExecutePaymentProvider;
import com.zbkj.paychannel.provider.PayProvider;
import com.zbkj.paychannel.provider.QueryProvider;
import com.zbkj.paychannel.provider.RefundProvider;

import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * 渠道×动作注册表。
 *
 * <p>相对 crmeb-pay-service 的 PaymentServiceFactory 的两点硬化：</p>
 * <ul>
 *   <li>构建期校验：重复注册 (channel, action) 直接失败；渠道缺 PAY 直接失败——
 *       配置错误在启动时暴露而不是运行时 NPE；</li>
 *   <li>运行期取用：缺失组合抛出带渠道与动作名的 PAYMENT_ACTION_UNSUPPORTED，
 *       而不是 registry.get(x).get(y) 的 NullPointerException。</li>
 * </ul>
 */
public final class ProviderRegistry {

    private final Map<String, Map<PayActionEnum, ChannelProvider>> registry = new HashMap<>();

    public ProviderRegistry(Collection<? extends ChannelProvider> providers) {
        if (providers == null || providers.isEmpty()) {
            throw new IllegalStateException("pay-channel-sdk: 未注册任何渠道 Provider");
        }
        for (ChannelProvider provider : providers) {
            register(provider);
        }
        validate();
    }

    private void register(ChannelProvider provider) {
        String channel = provider.channelCode();
        if (channel == null || channel.trim().isEmpty()) {
            throw new IllegalStateException(
                "pay-channel-sdk: Provider " + provider.getClass().getName() + " 的 channelCode 为空");
        }
        boolean anyAction = false;
        if (provider instanceof PayProvider) {
            put(channel, PayActionEnum.PAY, provider);
            anyAction = true;
        }
        if (provider instanceof QueryProvider) {
            put(channel, PayActionEnum.QUERY, provider);
            anyAction = true;
        }
        if (provider instanceof RefundProvider) {
            put(channel, PayActionEnum.REFUND, provider);
            anyAction = true;
        }
        if (provider instanceof CancelProvider) {
            put(channel, PayActionEnum.CANCEL, provider);
            anyAction = true;
        }
        if (provider instanceof CallbackProvider) {
            put(channel, PayActionEnum.CALLBACK, provider);
            anyAction = true;
        }
        if (provider instanceof ExecutePaymentProvider) {
            put(channel, PayActionEnum.EXECUTE_PAYMENT, provider);
            anyAction = true;
        }
        if (!anyAction) {
            throw new IllegalStateException("pay-channel-sdk: Provider " + provider.getClass().getName()
                + " 未实现任何动作接口（PayProvider/QueryProvider/...）");
        }
    }

    private void put(String channel, PayActionEnum action, ChannelProvider provider) {
        ChannelProvider previous =
            registry.computeIfAbsent(channel, k -> new EnumMap<>(PayActionEnum.class)).put(action, provider);
        if (previous != null && previous != provider) {
            throw new IllegalStateException(String.format(
                "pay-channel-sdk: 渠道 %s 的动作 %s 被重复注册：%s 与 %s",
                channel, action, previous.getClass().getName(), provider.getClass().getName()));
        }
    }

    private void validate() {
        for (Map.Entry<String, Map<PayActionEnum, ChannelProvider>> entry : registry.entrySet()) {
            if (!entry.getValue().containsKey(PayActionEnum.PAY)) {
                throw new IllegalStateException(String.format(
                    "pay-channel-sdk: 渠道 %s 未实现 PayProvider（PAY 为必选动作），已注册动作: %s",
                    entry.getKey(), entry.getValue().keySet()));
            }
        }
    }

    /** 渠道是否支持某动作 */
    public boolean supports(String channelCode, PayActionEnum action) {
        Map<PayActionEnum, ChannelProvider> actions = registry.get(channelCode);
        return actions != null && actions.containsKey(action);
    }

    /** 已注册渠道编码集合（有序，便于日志与自检） */
    public Set<String> channelCodes() {
        return new TreeSet<>(registry.keySet());
    }

    /**
     * 取用 Provider，缺失时抛出带渠道与动作名的明确异常。
     */
    @SuppressWarnings("unchecked")
    public <T extends ChannelProvider> T require(String channelCode, PayActionEnum action) {
        Map<PayActionEnum, ChannelProvider> actions = registry.get(channelCode);
        ChannelProvider provider = actions == null ? null : actions.get(action);
        if (provider == null) {
            throw new PayChannelException(PayErrorCode.PAYMENT_ACTION_UNSUPPORTED,
                "channel=" + channelCode + ", action=" + action);
        }
        return (T)provider;
    }
}
