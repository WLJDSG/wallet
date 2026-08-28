package com.wallet.app.config;

import com.wallet.channel.ChannelServiceImpl;
import com.wallet.channel.action.Channel;
import com.wallet.contract.channel.spi.CallLogWriter;
import com.wallet.contract.channel.spi.PayListener;
import com.wallet.contract.channel.spi.RefundStore;
import com.wallet.contract.channel.spi.TradeStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 渠道内核装配：收集所有渠道实现（MockChannel/AntomChannel 等），一次性注册。
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
