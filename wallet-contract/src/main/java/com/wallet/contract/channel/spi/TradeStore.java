package com.wallet.contract.channel.spi;

import com.wallet.common.enums.PayState;
import com.wallet.contract.channel.model.PayRequest;
import com.wallet.contract.channel.model.TradeInfo;

/**
 * 交易单持久化接口（调用方实现；钱包工程里映射到 pay_part 表的三方分段）。
 *
 * <p>关键契约——条件更新代替读改写：所有状态推进都通过 {@link #changeState}，
 * 实现必须是 {@code UPDATE ... SET state=:to WHERE out_trade_no=:no AND state=:from}
 * 式的条件更新并返回影响行数是否为 1。这是内核幂等与并发安全的基石，
 * 也让编排层无须把渠道 HTTP 调用包进数据库事务。</p>
 */
public interface TradeStore {

    /**
     * 创建交易单（状态 INIT），生成并回填 outTradeNo。
     *
     * @param request 支付请求
     * @param amount  实际请求渠道的金额，单位分（已含手续费）
     * @return 新交易单快照
     */
    TradeInfo create(PayRequest request, long amount);

    /**
     * 按 (channelCode, orderNo, outTradeNo) 查交易单。
     *
     * @return 不存在返回 null
     */
    TradeInfo find(String channelCode, String orderNo, String outTradeNo);

    /**
     * 条件推进状态：仅当当前状态等于 from 时更新为 to。
     *
     * @param thirdOutTradeNo 渠道侧交易号，非 null 时一并写入
     * @return 是否更新成功（false 表示已被并发推进，调用方按幂等跳过处理）
     */
    boolean changeState(String outTradeNo, PayState from, PayState to, String thirdOutTradeNo);

    /**
     * 退款成功后扣减可退金额（部分/全额）。
     * 实现应使用原子扣减（UPDATE ... SET refundable = refundable - :amount WHERE refundable >= :amount）。
     */
    void applyRefund(String outTradeNo, long amount, boolean allRefund);
}
