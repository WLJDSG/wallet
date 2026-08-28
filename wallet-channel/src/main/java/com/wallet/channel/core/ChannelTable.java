package com.wallet.channel.core;

import com.wallet.channel.action.CallbackAction;
import com.wallet.channel.action.CancelAction;
import com.wallet.channel.action.Channel;
import com.wallet.channel.action.ConfirmAction;
import com.wallet.channel.action.PayAction;
import com.wallet.channel.action.QueryAction;
import com.wallet.channel.action.RefundAction;
import com.wallet.contract.channel.enums.ActionType;
import com.wallet.contract.channel.enums.PayError;
import com.wallet.contract.channel.error.ChannelException;

import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * 渠道×动作注册表。
 *
 * <p>两点硬化（相对 crmeb-pay-service 的 PaymentServiceFactory）：</p>
 * <ul>
 *   <li>构建期校验：重复注册 (channel, action) 直接失败；渠道缺 PAY 直接失败——
 *       配置错误在启动时暴露而不是运行时 NPE；</li>
 *   <li>运行期取用：缺失组合抛出带渠道与动作名的 PAYMENT_ACTION_UNSUPPORTED。</li>
 * </ul>
 */
public final class ChannelTable {

    private final Map<String, Map<ActionType, Channel>> table = new HashMap<>();

    public ChannelTable(Collection<? extends Channel> channels) {
        if (channels == null || channels.isEmpty()) {
            throw new IllegalStateException("wallet-channel: 未注册任何渠道");
        }
        for (Channel channel : channels) {
            register(channel);
        }
        validate();
    }

    private void register(Channel channel) {
        String code = channel.code();
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalStateException(
                "wallet-channel: 渠道 " + channel.getClass().getName() + " 的 code 为空");
        }
        boolean anyAction = false;
        if (channel instanceof PayAction) {
            put(code, ActionType.PAY, channel);
            anyAction = true;
        }
        if (channel instanceof QueryAction) {
            put(code, ActionType.QUERY, channel);
            anyAction = true;
        }
        if (channel instanceof RefundAction) {
            put(code, ActionType.REFUND, channel);
            anyAction = true;
        }
        if (channel instanceof CancelAction) {
            put(code, ActionType.CANCEL, channel);
            anyAction = true;
        }
        if (channel instanceof CallbackAction) {
            put(code, ActionType.CALLBACK, channel);
            anyAction = true;
        }
        if (channel instanceof ConfirmAction) {
            put(code, ActionType.CONFIRM, channel);
            anyAction = true;
        }
        if (!anyAction) {
            throw new IllegalStateException("wallet-channel: 渠道 " + channel.getClass().getName()
                + " 未实现任何动作接口（PayAction/QueryAction/...）");
        }
    }

    private void put(String code, ActionType action, Channel channel) {
        Channel previous =
            table.computeIfAbsent(code, k -> new EnumMap<>(ActionType.class)).put(action, channel);
        if (previous != null && previous != channel) {
            throw new IllegalStateException(String.format(
                "wallet-channel: 渠道 %s 的动作 %s 被重复注册：%s 与 %s",
                code, action, previous.getClass().getName(), channel.getClass().getName()));
        }
    }

    private void validate() {
        for (Map.Entry<String, Map<ActionType, Channel>> entry : table.entrySet()) {
            if (!entry.getValue().containsKey(ActionType.PAY)) {
                throw new IllegalStateException(String.format(
                    "wallet-channel: 渠道 %s 未实现 PayAction（PAY 为必选动作），已注册动作: %s",
                    entry.getKey(), entry.getValue().keySet()));
            }
        }
    }

    /** 渠道是否支持某动作 */
    public boolean supports(String channelCode, ActionType action) {
        Map<ActionType, Channel> actions = table.get(channelCode);
        return actions != null && actions.containsKey(action);
    }

    /** 已注册渠道编码集合（有序，便于日志与自检） */
    public Set<String> channelCodes() {
        return new TreeSet<>(table.keySet());
    }

    /**
     * 取用渠道实现，缺失时抛出带渠道与动作名的明确异常。
     */
    @SuppressWarnings("unchecked")
    public <T extends Channel> T require(String channelCode, ActionType action) {
        Map<ActionType, Channel> actions = table.get(channelCode);
        Channel channel = actions == null ? null : actions.get(action);
        if (channel == null) {
            throw new ChannelException(PayError.PAYMENT_ACTION_UNSUPPORTED,
                "channel=" + channelCode + ", action=" + action);
        }
        return (T) channel;
    }
}
