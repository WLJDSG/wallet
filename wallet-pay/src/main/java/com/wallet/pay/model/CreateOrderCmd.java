package com.wallet.pay.model;

import java.util.List;

/**
 * 创建拆分支付单命令（服务层入参，基础校验在 wallet-app 的 CreateOrderReq）。
 * 金额勾稽（sum(段金额)=总额）等业务规则在服务层校验。
 *
 * @param bizOrderNo 外部业务单号
 * @param totalAmount 应付总额，单位分（= sum(分段金额)）
 * @param currency   币种
 * @param parts      支付分段
 */
public record CreateOrderCmd(String bizOrderNo, long totalAmount, String currency, List<PartItem> parts) {
}
