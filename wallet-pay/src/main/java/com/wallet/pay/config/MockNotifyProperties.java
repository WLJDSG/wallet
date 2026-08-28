package com.wallet.pay.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * mock 自动回调配置（application.yml 的 wallet.mock.*，与 channel 的 MockProperties 同前缀各取所需字段）。
 * mock 渠道在下单后 N 秒模拟推送支付结果，便于联调。
 */
@ConfigurationProperties(prefix = "wallet.mock")
public class MockNotifyProperties {

    /** 下单后 N 秒自动回调；0 = 不自动，手工触发回调接口 */
    private int notifySeconds = 5;

    /** mock 回调验签 token（与 channel 侧 MockProperties.secret 同值） */
    private String secret = "mock-secret";

    public int getNotifySeconds() {
        return notifySeconds;
    }

    public void setNotifySeconds(int notifySeconds) {
        this.notifySeconds = notifySeconds;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }
}
