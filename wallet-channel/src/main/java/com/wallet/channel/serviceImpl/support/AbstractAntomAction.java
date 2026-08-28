package com.wallet.channel.serviceImpl.support;

import com.wallet.contract.channel.action.Channel;

/**
 * Antom 渠道动作基类：渠道编码 + 注入共享 {@link AntomClient}。
 * 每个动作（PAY/QUERY/REFUND/CANCEL/CALLBACK）一个实现类，编排层按 渠道×动作 两维分发。
 */
public abstract class AbstractAntomAction implements Channel {

    protected final AntomClient client;

    protected AbstractAntomAction(AntomClient client) {
        this.client = client;
    }

    @Override
    public String code() {
        return AntomClient.CHANNEL_CODE;
    }
}
