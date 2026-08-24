package com.wallet.pay.validate;

import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * 支付域校验责任链：收集全部 {@link PayValidator} Bean 按 order 排序，
 * 执行时按场景过滤依次校验，任一校验器抛异常即中断。
 * 支付/退款/查询/取消等入口统一走这里，校验逻辑不再散落在服务方法开头。
 */
@Component
public class PayValidatorChain {

    private final List<PayValidator> validators;

    public PayValidatorChain(List<PayValidator> validators) {
        this.validators = validators.stream()
            .sorted(Comparator.comparingInt(PayValidator::order))
            .toList();
    }

    public void validate(PayValidationContext context) {
        for (PayValidator validator : validators) {
            if (validator.scenes().contains(context.scene())) {
                validator.validate(context);
            }
        }
    }
}
