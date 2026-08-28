package com.wallet.app.config;

import com.wallet.channel.serviceImpl.support.ChannelServiceImpl;
import com.wallet.contract.channel.action.Channel;
import com.wallet.contract.channel.spi.CallLogWriter;
import com.wallet.contract.channel.spi.PayListener;
import com.wallet.contract.channel.spi.RefundStore;
import com.wallet.contract.channel.spi.TradeStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 渠道内核装配：收集所有渠道动作实现（AntomPayAction/MockPayAction 等，按 渠道×动作 注册），一次性装配。
 * 配置错误（渠道缺 PAY/重复注册）在启动期失败。
 */
@Configuration
public class ChannelConfig {

    @Bean
    public ChannelServiceImpl channelKit(List<Channel> channels, TradeStore tradeStore, RefundStore refundStore,
        PayListener payListener, CallLogWriter callLogWriter) {
        return ChannelServiceImpl.builder()
            .channels(channels)
            .tradeStore(tradeStore)
            .refundStore(refundStore)
            .listener(payListener)
            .logWriter(callLogWriter)
            .build();
    }
}
