package com.wallet.common.rule;

/**
 * 通用规则：对某类事实（fact）求值，可校验（不满足抛业务异常否决）或改写结果。
 *
 * <p>各业务域定义自己的子接口固定泛型（如券域 {@code CouponDeductRule extends Rule<CouponFact, Long>}、
 * 将来活动域 {@code PromotionRule extends Rule<PromotionFact, PromotionResult>}），
 * 规则实现注册为 Spring Bean，由域引擎收集后交 {@link RulePipeline} 执行。
 * 新增规则 = 新增一个实现类，引擎与调用方零改动。</p>
 *
 * @param <F> 事实类型（规则的输入上下文）
 * @param <R> 结果类型（沿管道传递、逐条规则改写）
 */
public interface Rule<F, R> {

    /** 本规则是否适用该事实（不适用则跳过） */
    boolean matches(F fact);

    /**
     * 执行规则：基于当前结果返回新结果；校验型规则不满足时抛业务异常否决整条管道。
     *
     * @param fact    事实
     * @param current 上一条规则产出的结果
     * @return 新结果
     */
    R apply(F fact, R current);

    /** 执行顺序，小的先执行（约定：校验 10~/计算 20~/封顶 30~/兜底 40~） */
    default int order() {
        return 100;
    }
}
