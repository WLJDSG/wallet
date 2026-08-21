package com.wallet.pay.model;

import java.util.List;

/**
 * 创建拆分支付单请求。
 *
 * @param bizOrderNo 外部业务单号
 * @param totalAmount 应付总额，单位分（= sum(分段金额)）
 * @param currency   币种
 * @param parts      支付分段
 */
public record CreateOrderCmd(String bizOrderNo, long totalAmount, String currency, List<PartItem> parts) {
}
