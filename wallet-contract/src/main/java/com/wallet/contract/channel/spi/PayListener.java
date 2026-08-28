package com.wallet.contract.channel.spi;

/**
 * 支付事件监听接口（调用方实现，衔接订单履约/账务等下游）。
 *
 * <p>语义保证：</p>
 * <ul>
 *   <li>onPaySuccess 对同一交易号至多触发一次（以 changeState 的 CAS 成功为准，
 *       回调与轮询并发到达也只触发一次）；</li>
 *   <li>监听器内抛异常会向上传播（如需"支付状态已落库、下游异步重试"，
 *       应在实现里转投消息队列而不是同步做重业务）。</li>
 * </ul>
 */
public interface PayListener {

    void onPaySuccess(String channelCode, String orderNo, String outTradeNo);

    /** @param amount 退款金额，单位分 */
    void onRefundSuccess(String channelCode, String orderNo, String refundOrderNo, long amount);
}
