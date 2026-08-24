package com.wallet.common.rule;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 规则管道执行器（纯执行器，不依赖 Spring）：按 order 升序依次执行 matches 的规则，
 * 结果沿管道传递。构造时排序一次，线程安全可复用。
 */
public final class RulePipeline<F, R> {

    private final List<Rule<F, R>> rules;

    private RulePipeline(List<Rule<F, R>> rules) {
        this.rules = rules;
    }

    public static <F, R> RulePipeline<F, R> of(List<? extends Rule<F, R>> rules) {
        List<Rule<F, R>> sorted = new ArrayList<>(rules);
        sorted.sort(Comparator.comparingInt(Rule::order));
        return new RulePipeline<>(sorted);
    }

    /**
     * @param fact    事实
     * @param initial 初始结果
     * @return 全部适用规则执行后的最终结果
     */
    public R evaluate(F fact, R initial) {
        R result = initial;
        for (Rule<F, R> rule : rules) {
            if (rule.matches(fact)) {
                result = rule.apply(fact, result);
            }
        }
        return result;
    }
}
