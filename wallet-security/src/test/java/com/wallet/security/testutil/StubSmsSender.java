package com.wallet.security.testutil;

import com.wallet.security.spi.SmsSender;

/**
 * 记录发送参数的短信发送测试替身。
 */
public class StubSmsSender implements SmsSender {

    public String lastPhone;
    public String lastCode;
    public long lastTtlSeconds;
    public int sendCount;
    public boolean result = true;
    public RuntimeException throwOnSend;

    @Override
    public boolean sendVerificationCode(String phone, String code, long codeTtlSeconds) {
        if (throwOnSend != null) {
            throw throwOnSend;
        }
        this.lastPhone = phone;
        this.lastCode = code;
        this.lastTtlSeconds = codeTtlSeconds;
        this.sendCount++;
        return result;
    }
}
