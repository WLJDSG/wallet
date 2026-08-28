package com.wallet.pay.validate;

import java.util.Set;

/**
 * 支付域校验器（责任链节点）：声明适用场景与顺序，校验不通过抛业务异常中断链。
 * 实现类注册为 Spring Bean 即自动进入 {@link PayValidatorChain}，
 * 新增校验 = 新增一个实现类，服务层零改动。
 */
public interface PayValidator {

    /** 适用的校验场景 */
    Set<PayScene> scenes();

    /** 校验，不通过抛 CommonException（约定：归属 10 / 状态 20 / 明细 30） */
    void validate(PayValidationContext context);

    /** 链内顺序，小的先执行 */
    default int order() {
        return 100;
    }
}
