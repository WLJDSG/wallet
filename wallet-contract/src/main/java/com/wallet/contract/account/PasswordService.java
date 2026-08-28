package com.wallet.contract.account;

/**
 * 支付密码能力契约。由 {@code wallet-account} 的 {@code PasswordService} 实现。
 *
 * <p>三要素：BCrypt 慢哈希、错误锁定（连续错 N 次或当日错 M 次锁 10 分钟）、
 * 一次性授权票据（校验通过签发，提交支付时原子消费并复核用户/订单/金额）。</p>
 */
public interface PasswordService {

    /** 设置/重置支付密码。已设置时必须校验旧密码。 */
    void set(Long userId, String password, String oldPassword);

    /**
     * 校验支付密码并签发一次性授权票据。
     *
     * @return 票据
     */
    String verifyAndIssue(Long userId, String password, String orderNo, long amount);

    /** 原子消费一次性票据并复核用户/订单/金额三者一致。 */
    void consumeTicket(String ticket, Long userId, String orderNo, long amount);
}
