package com.wallet.account.service.coupon;

import com.wallet.common.rule.Rule;

/**
 * 券抵扣规则（券域对通用规则引擎的泛型固化）：事实 = 券+订单，结果 = 抵扣金额（分）。
 * 实现类注册为 Spring Bean 即自动进入 {@link CouponRuleEngine} 的管道，
 * 新增券玩法（如新券类型、活动加成）只需新增实现类。
 */
public interface CouponDeductRule extends Rule<CouponFact, Long> {
}
