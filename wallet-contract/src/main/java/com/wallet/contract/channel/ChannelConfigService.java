package com.wallet.contract.channel;

/**
 * 渠道配置提供者（扩展点）。宿主实现（如读取渠道配置存储表），
 * 渠道实现经此接口取自己的商户密钥等配置，不感知配置存哪。
 */
public interface ChannelConfigService {

    /** 渠道是否已配置且启用（不抛异常的探测版）。 */
    boolean isEnabled(String channelCode);

    /**
     * 取启用状态的渠道配置并反序列化为渠道自定义类型。
     *
     * @throws com.wallet.contract.channel.error.ChannelException 未配置/停用/JSON 非法时抛出
     */
    <T> T requireEnabled(String channelCode, Class<T> type);
}
