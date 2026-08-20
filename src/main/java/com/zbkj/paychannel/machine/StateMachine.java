package com.zbkj.paychannel.machine;

/**
 * 转换表式状态机契约。
 *
 * <p>刻意不使用 GoF 状态模式（每状态一类），转换规则以数据表形式集中声明，
 * 保证全部合法流转一目了然。</p>
 */
public interface StateMachine<S, E> {

    /**
     * 执行状态转换。
     *
     * @return 目标状态
     * @throws com.zbkj.paychannel.exception.PayChannelException 流转不合法时抛出 ILLEGAL_CHANGE_STATUS
     */
    S transition(S source, E event);

    /** 判断流转是否合法（幂等检查的基础：终态对任何事件返回 false） */
    boolean canTransition(S source, E event);
}
