package com.wallet.asset.service;

import lombok.AllArgsConstructor;
import com.wallet.asset.entity.PointLog;
import com.wallet.asset.error.AssetError;
import com.wallet.asset.mapper.AccountMapper;
import com.wallet.asset.mapper.PointLogMapper;
import com.wallet.common.error.BizException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 积分服务（与余额同款：CAS + 流水幂等）。
 *
 * <p>变动类型：ADD 发放 / PAY 支付扣减 / ROLLBACK 支付回滚 / REFUND 退款返还。</p>
 */
@Service
@AllArgsConstructor
public class PointService {

    public static final String TYPE_ADD = "ADD";
    public static final String TYPE_PAY = "PAY";
    public static final String TYPE_ROLLBACK = "ROLLBACK";
    public static final String TYPE_REFUND = "REFUND";

    private final AccountMapper accountMapper;
    private final PointLogMapper pointLogMapper;


    /** 发放积分。 */
    @Transactional
    public long add(Long userId, long count, String bizNo, String remark) {
        return change(userId, count, TYPE_ADD, bizNo, null, remark);
    }

    /** 支付扣减。bizNo 用支付分段号。 */
    @Transactional
    public long pay(Long userId, long count, String bizNo, String orderNo, String remark) {
        return change(userId, -count, TYPE_PAY, bizNo, orderNo, remark);
    }

    /** 支付未完成时的补偿返还。 */
    @Transactional
    public long rollback(Long userId, long count, String bizNo, String orderNo, String remark) {
        return change(userId, count, TYPE_ROLLBACK, bizNo, orderNo, remark);
    }

    /** 退款返还。bizNo 用退款分段号。 */
    @Transactional
    public long refund(Long userId, long count, String bizNo, String orderNo, String remark) {
        return change(userId, count, TYPE_REFUND, bizNo, orderNo, remark);
    }

    private long change(Long userId, long delta, String type, String bizNo, String orderNo, String remark) {
        PointLog exist = pointLogMapper.findByBizAndType(bizNo, type);
        if (exist != null) {
            return exist.getAfterCount(); // 幂等重放
        }
        accountMapper.createIfMissing(userId);
        int rows;
        if (delta >= 0) {
            rows = accountMapper.increasePoint(userId, delta);
        } else {
            rows = accountMapper.decreasePoint(userId, -delta);
        }
        if (rows == 0) {
            throw new BizException(AssetError.POINT_NOT_ENOUGH, "userId=" + userId + ", count=" + (-delta));
        }
        long after = accountMapper.selectPoint(userId);
        pointLogMapper.insertIgnore(new PointLog(userId, bizNo, type, delta, after, orderNo, remark));
        return after;
    }
}
