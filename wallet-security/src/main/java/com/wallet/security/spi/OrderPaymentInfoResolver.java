package com.wallet.security.spi;

import com.wallet.security.model.OrderPaymentInfo;

/**
 * 宿主订单支付信息解析端口。支付安全内核 通过它在签发与消费票据时核对订单归属、
 * 支付状态、金额与币种；订单、余额、账务与业务状态流转始终归宿主所有。
 *
 * <p>实现契约：</p>
 * <ul>
 *   <li>订单不存在或不属于 uid：返回 null（支付安全内核 统一抛 ORDER_DOES_NOT_EXIST）。</li>
 *   <li>orderType 不支持：抛出错误码为 PAY_SECURITY_ORDER_TYPE_UNSUPPORTED 的
 *       {@link com.wallet.security.error.PaySecurityException}。</li>
 *   <li>amount 必须是“本次余额应付全额”（含宿主定义的附加费聚合），不得返回部分金额。</li>
 *   <li>实现必须实时读取订单，不得返回缓存快照。</li>
 * </ul>
 */
public interface OrderPaymentInfoResolver {

    /**
     * 解析订单支付信息。
     *
     * @param uid 当前用户ID
     * @param orderNo 订单号
     * @param orderType 订单类型
     * @return 订单支付信息；订单不存在或不归属该用户时返回 null
     */
    OrderPaymentInfo resolve(Long uid, String orderNo, String orderType);
}
