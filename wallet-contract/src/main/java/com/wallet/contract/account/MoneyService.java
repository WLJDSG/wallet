package com.wallet.contract.account;

/**
 * 余额能力契约。由 {@code wallet-account} 的 {@code MoneyService} 实现，支付编排与 Web 层经此调用。
 *
 * <p>并发约定：调用方应持有该支付单分布式锁；流水按 (bizNo, type) 幂等，
 * 重复调用返回既有流水结果，不重复扣减。变动类型：RECHARGE 充值 / PAY 支付扣减 /
 * ROLLBACK 支付回滚 / REFUND 退款返还。</p>
 */
public interface MoneyService {

    /** 充值（模拟入金）。bizNo 用充值流水号。返回变动后余额。 */
    long recharge(Long userId, long amount, String bizNo, String remark);

    /** 支付扣减。bizNo 用支付分段号。返回变动后余额。 */
    long pay(Long userId, long amount, String bizNo, String orderNo, String remark);

    /** 支付未完成时的补偿返还。bizNo 用支付分段号。返回变动后余额。 */
    long rollback(Long userId, long amount, String bizNo, String orderNo, String remark);

    /** 退款返还。bizNo 用退款分段号。返回变动后余额。 */
    long refund(Long userId, long amount, String bizNo, String orderNo, String remark);
}
