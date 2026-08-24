package com.wallet.asset.service.coupon;

import com.wallet.asset.entity.UserCoupon;
import com.wallet.common.rule.RulePipeline;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 券规则引擎：收集全部 {@link CouponDeductRule} Bean 组成管道
 * （校验 10~ → 计算 20~ → 封顶 30~ → 兜底 40~），计算券在订单上的应抵扣额。
 *
 * <p>这是通用规则引擎（common 的 Rule/RulePipeline）的第一个接入域；
 * 后续营销活动等规则照此模式：定义域 fact + 域规则子接口 + 若干 @Component 规则实现。</p>
 */
@Component
public class CouponRuleEngine {

    private final RulePipeline<CouponFact, Long> pipeline;

    public CouponRuleEngine(List<CouponDeductRule> rules) {
        this.pipeline = RulePipeline.of(rules);
    }

    /**
     * 计算券在该订单上的应抵扣金额（单位分）；不满足使用条件抛业务异常。
     * 券的归属/状态/过期校验在 CouponService.checkUsable，此处只管金额规则。
     */
    public long calcDeduct(UserCoupon coupon, long orderAmount) {
        return pipeline.evaluate(new CouponFact(coupon, orderAmount), 0L);
    }
}
