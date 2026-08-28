package com.wallet.contract.channel.action;

/**
 * 渠道根接口。
 *
 * <p>一个渠道按动作实现若干子接口（PayAction 必须实现，其余按渠道能力选实现），
 * 同一个类可同时实现多个动作接口，注册表按接口类型归类到 (code, action) 二维表。</p>
 */
public interface Channel {

    /**
     * 渠道编码，全局唯一（建议大写，如 "MOCK"、"ANTOM"）。
     * 内核不内置渠道枚举：渠道集合由使用方自行定义并保证与此处一致。
     */
    String code();
}
