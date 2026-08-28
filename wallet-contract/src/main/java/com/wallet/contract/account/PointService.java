package com.wallet.contract.account;

/**
 * 积分能力契约。由 {@code wallet-account} 的 {@code PointService} 实现，支付编排与 Web 层经此调用。
 *
 * <p>与余额同款：CAS + 流水幂等。变动类型：ADD 发放 / PAY 支付扣减 /
 * ROLLBACK 支付回滚 / REFUND 退款返还。</p>
 */
public interface PointService {

    /** 发放积分。返回变动后积分。 */
    long add(Long userId, long count, String bizNo, String remark);

    /** 支付扣减。bizNo 用支付分段号。返回变动后积分。 */
    long pay(Long userId, long count, String bizNo, String orderNo, String remark);

    /** 支付未完成时的补偿返还。返回变动后积分。 */
    long rollback(Long userId, long count, String bizNo, String orderNo, String remark);

    /** 退款返还。bizNo 用退款分段号。返回变动后积分。 */
    long refund(Long userId, long count, String bizNo, String orderNo, String remark);
}
