package com.wallet.security.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 宿主订单支付信息快照，由 {@link com.wallet.security.spi.OrderPaymentInfoResolver} 返回。
 *
 * <p>amount 必须是本次余额应付全额（含宿主定义的附加费聚合），currency 由宿主决定。
 * 支付安全内核 只消费这些支付信息，不理解订单业务状态机。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderPaymentInfo {

    /** 订单号。 */
    private String orderNo;

    /** 订单类型（宿主定义的类型编码）。 */
    private String orderType;

    /** 本次余额应付全额。 */
    private BigDecimal amount;

    /** 币种。 */
    private String currency;

    /** 是否已支付。 */
    private Boolean paid;
}
