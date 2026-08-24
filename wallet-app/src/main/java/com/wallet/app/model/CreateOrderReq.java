package com.wallet.app.model;

import com.wallet.pay.model.CreateOrderCmd;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;

import java.util.List;

/**
 * 创建拆分支付单入参。金额勾稽（sum(段金额)=总额）等业务规则在服务层校验。
 *
 * @param bizOrderNo 外部业务单号
 * @param totalAmount 应付总额，单位分（= sum(分段金额)）
 * @param currency   币种
 * @param parts      支付分段
 */
public record CreateOrderReq(@NotBlank(message = "业务单号不能为空") String bizOrderNo,
                             @Positive(message = "总金额必须大于 0") long totalAmount,
                             @NotBlank(message = "币种不能为空") String currency,
                             @NotEmpty(message = "至少一个支付分段") List<@Valid PartItemReq> parts) {

    public CreateOrderCmd toCmd() {
        return new CreateOrderCmd(bizOrderNo, totalAmount, currency,
            parts.stream().map(PartItemReq::toCmd).toList());
    }
}
