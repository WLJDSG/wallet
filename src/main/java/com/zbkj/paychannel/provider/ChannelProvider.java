package com.zbkj.paychannel.provider;

/**
 * 渠道 Provider 根接口。
 *
 * <p>一个渠道按动作实现若干子接口（PayProvider 必须实现，其余按渠道能力选实现），
 * 同一个类可同时实现多个动作接口，注册表按接口类型归类到 (channelCode, action) 二维注册表。</p>
 *
 * <p>与 crmeb-pay-service 的差异：不再有 buildParams/buildResponse 三段式模板——
 * 该模板的主要目的是给 AOP 日志留钩子，本 SDK 的日志由编排层统一采集，
 * Provider 只需一个语义完整的方法，杜绝"在 buildParams 里验签、在 buildResponse 里抛业务异常"的越位空间。</p>
 */
public interface ChannelProvider {

    /**
     * 渠道编码，宿主全局唯一（建议大写，如 "JKOPAY"、"ANTOM"、"WEIXIN_WISE"）。
     * SDK 不内置渠道枚举：各商城渠道集合不同，枚举由宿主自行定义并保证与此处一致。
     */
    String channelCode();
}
