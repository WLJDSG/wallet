package com.wallet.channel.mock;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * mock 渠道行为配置（application.yml 的 wallet.mock.*）。
 * 自动回调相关（notifySeconds）由宿主侧的 MockNotifyProperties 读取，本类只管渠道行为。
 */
@ConfigurationProperties(prefix = "wallet.mock")
public class MockProperties {

    /** mock 渠道总开关：false 时 MockChannel 不注册（生产建议关闭或直接移除 config/mock.yml） */
    private boolean enabled = true;

    /** 下单/查询延迟毫秒（模拟网络耗时，验证锁内耗时处理） */
    private long delayMs = 100;

    /** 下单失败概率 0~1（模拟渠道偶发失败） */
    private double failRate = 0.0;

    /** 退款强制失败开关（演练退款 FAIL 路径） */
    private boolean refundFail = false;

    /** mock 回调验签 token */
    private String secret = "mock-secret";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getDelayMs() {
        return delayMs;
    }

    public void setDelayMs(long delayMs) {
        this.delayMs = delayMs;
    }

    public double getFailRate() {
        return failRate;
    }

    public void setFailRate(double failRate) {
        this.failRate = failRate;
    }

    public boolean isRefundFail() {
        return refundFail;
    }

    public void setRefundFail(boolean refundFail) {
        this.refundFail = refundFail;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }
}
