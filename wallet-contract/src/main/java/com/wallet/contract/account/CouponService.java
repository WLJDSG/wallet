package com.wallet.contract.account;

import com.wallet.contract.account.model.CouponView;

import java.util.List;

/**
 * 优惠券能力契约。由 {@code wallet-account} 的 {@code CouponService} 实现，支付编排与 Web 层经此调用。
 *
 * <p>领券 / 核销 / 返还全部条件更新（CAS）。跨边界只暴露 {@link CouponView} 快照，
 * 不暴露持久化实体。</p>
 */
public interface CouponService {

    /** 领券：模板未超发行量才成功。返回用户券快照。 */
    CouponView take(Long userId, Long couponId);

    /**
     * 校验券可用并经规则引擎计算应抵扣额（单位分）：
     * 满减=面额、折扣=按折扣率、统一受最低消费/最高抵扣/不超订单额约束。
     */
    long evaluateDeduct(Long userId, Long userCouponId, long orderAmount);

    /** 核销（条件更新：未使用且未过期才成功）。 */
    void use(Long userId, Long userCouponId, String orderNo);

    /**
     * 返还（支付未完成补偿时调用）。券已过期则返回 false，不恢复。
     *
     * @return 是否成功恢复为未使用
     */
    boolean restore(Long userId, Long userCouponId, String orderNo);

    /** 用户可用券列表。 */
    List<CouponView> usable(Long userId);
}
