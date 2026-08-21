package com.wallet.pay.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 支付编排配置（application.yml 的 wallet.pay.*）。
 */
@ConfigurationProperties(prefix = "wallet.pay")
public class PayConfig {

    /** 支付单超时分钟数（超时关单任务扫描） */
    private int expireMinutes = 15;

    /** 积分汇率：多少积分抵 1 元 */
    private int pointsPerYuan = 100;

    public int getExpireMinutes() {
        return expireMinutes;
    }

    public void setExpireMinutes(int expireMinutes) {
        this.expireMinutes = expireMinutes;
    }

    public int getPointsPerYuan() {
        return pointsPerYuan;
    }

    public void setPointsPerYuan(int pointsPerYuan) {
        this.pointsPerYuan = pointsPerYuan;
    }
}
