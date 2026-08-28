package com.wallet.channel.serviceImpl.support;

import com.wallet.contract.channel.action.Channel;

/**
 * mock 渠道动作基类：渠道编码 + 共享 store/props + 延迟模拟。
 * 每个动作（PAY/QUERY/REFUND/CANCEL/CALLBACK）一个实现类，编排层按 渠道×动作 两维分发。
 */
public abstract class AbstractMockAction implements Channel {

    public static final String CHANNEL_CODE = "MOCK";

    protected final MockStore store;
    protected final MockProperties props;

    protected AbstractMockAction(MockStore store, MockProperties props) {
        this.store = store;
        this.props = props;
    }

    @Override
    public String code() {
        return CHANNEL_CODE;
    }

    protected void sleep() {
        if (props.getDelayMs() > 0) {
            try {
                Thread.sleep(props.getDelayMs());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
