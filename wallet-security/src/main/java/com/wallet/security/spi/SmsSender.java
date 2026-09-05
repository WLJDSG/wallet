package com.wallet.security.spi;

/**
 * 支付安全短信发送端口。
 *
 * <p>验证码由 支付安全内核 生成并存储在自己的 KV 键下，宿主只负责把给定验证码发送出去，
 * 不得再自行生成、存储验证码或复用宿主通用验证码存储。</p>
 *
 * <p>实现契约：发送失败返回 false（支付安全内核 转为 PAY_SECURITY_UNAVAILABLE 并回收已存验证码）；
 * 实现抛出的宿主异常（如短信余量不足）原样向上穿透。</p>
 */
public interface SmsSender {

    /**
     * 发送支付安全验证码短信。
     *
     * @param phone 手机号
     * @param code 待发送的验证码
     * @param codeTtlSeconds 验证码有效秒数，供短信模板展示有效期
     * @return 发送成功返回 true
     */
    boolean sendVerificationCode(String phone, String code, long codeTtlSeconds);
}
