package com.wallet.account.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 支付密码策略配置（application.yml 的 wallet.password.*）。
 */
@ConfigurationProperties(prefix = "wallet.password")
public class PasswordProperties {

    /** 连续错误几次锁定 */
    private int maxContinuousFail = 5;

    /** 当日错误几次锁定 */
    private int maxDailyFail = 10;

    /** 锁定分钟数 */
    private int lockMinutes = 10;

    /** 一次性票据有效期（秒） */
    private int ticketTtlSeconds = 300;

    /** BCrypt 强度（成本因子） */
    private int bcryptStrength = 12;

    public int getMaxContinuousFail() {
        return maxContinuousFail;
    }

    public void setMaxContinuousFail(int maxContinuousFail) {
        this.maxContinuousFail = maxContinuousFail;
    }

    public int getMaxDailyFail() {
        return maxDailyFail;
    }

    public void setMaxDailyFail(int maxDailyFail) {
        this.maxDailyFail = maxDailyFail;
    }

    public int getLockMinutes() {
        return lockMinutes;
    }

    public void setLockMinutes(int lockMinutes) {
        this.lockMinutes = lockMinutes;
    }

    public int getTicketTtlSeconds() {
        return ticketTtlSeconds;
    }

    public void setTicketTtlSeconds(int ticketTtlSeconds) {
        this.ticketTtlSeconds = ticketTtlSeconds;
    }

    public int getBcryptStrength() {
        return bcryptStrength;
    }

    public void setBcryptStrength(int bcryptStrength) {
        this.bcryptStrength = bcryptStrength;
    }
}
