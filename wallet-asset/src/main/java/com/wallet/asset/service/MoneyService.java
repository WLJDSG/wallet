package com.wallet.asset.service;

import com.wallet.asset.entity.MoneyLog;
import com.wallet.asset.error.AssetError;
import com.wallet.asset.mapper.AccountMapper;
import com.wallet.asset.mapper.MoneyLogMapper;
import com.wallet.common.error.BizException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 余额服务。
 *
 * <p>并发约定：本类方法应在调用方持有该支付单分布式锁的前提下调用（钱包工程统一一把锁），
 * 流水按 (bizNo, type) 幂等——重复调用返回既有流水结果，不重复扣减。</p>
 *
 * <p>变动类型：RECHARGE 充值 / PAY 支付扣减 / ROLLBACK 支付回滚 / REFUND 退款返还。</p>
 */
@Service
public class MoneyService {

    public static final String TYPE_RECHARGE = "RECHARGE";
    public static final String TYPE_PAY = "PAY";
    public static final String TYPE_ROLLBACK = "ROLLBACK";
    public static final String TYPE_REFUND = "REFUND";

    private final AccountMapper accountMapper;
    private final MoneyLogMapper moneyLogMapper;

    public MoneyService(AccountMapper accountMapper, MoneyLogMapper moneyLogMapper) {
        this.accountMapper = accountMapper;
        this.moneyLogMapper = moneyLogMapper;
    }

    /** 充值（模拟入金）。bizNo 用充值流水号。 */
    @Transactional
    public long recharge(Long userId, long amount, String bizNo, String remark) {
        return change(userId, amount, TYPE_RECHARGE, bizNo, null, remark);
    }

    /** 支付扣减。bizNo 用支付分段号。 */
    @Transactional
    public long pay(Long userId, long amount, String bizNo, String orderNo, String remark) {
        return change(userId, -amount, TYPE_PAY, bizNo, orderNo, remark);
    }

    /** 支付未完成时的补偿返还。bizNo 用支付分段号。 */
    @Transactional
    public long rollback(Long userId, long amount, String bizNo, String orderNo, String remark) {
        return change(userId, amount, TYPE_ROLLBACK, bizNo, orderNo, remark);
    }

    /** 退款返还。bizNo 用退款分段号。 */
    @Transactional
    public long refund(Long userId, long amount, String bizNo, String orderNo, String remark) {
        return change(userId, amount, TYPE_REFUND, bizNo, orderNo, remark);
    }

    /** 统一变更：delta 正数加、负数减。返回变动后余额快照。 */
    private long change(Long userId, long delta, String type, String bizNo, String orderNo, String remark) {
        MoneyLog exist = moneyLogMapper.findByBizAndType(bizNo, type);
        if (exist != null) {
            return exist.getAfterAmount(); // 幂等重放
        }
        accountMapper.createIfMissing(userId);
        int rows;
        if (delta >= 0) {
            rows = accountMapper.increaseMoney(userId, delta);
        } else {
            rows = accountMapper.decreaseMoney(userId, -delta);
        }
        if (rows == 0) {
            throw new BizException(AssetError.MONEY_NOT_ENOUGH,
                "userId=" + userId + ", amount=" + (-delta));
        }
        long after = accountMapper.selectMoney(userId);
        moneyLogMapper.insertIgnore(new MoneyLog(userId, bizNo, type, delta, after, orderNo, remark));
        return after;
    }
}
