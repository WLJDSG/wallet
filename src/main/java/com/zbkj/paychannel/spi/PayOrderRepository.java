package com.zbkj.paychannel.spi;

import com.zbkj.paychannel.enums.PayStateEnum;
import com.zbkj.paychannel.model.PayCommand;
import com.zbkj.paychannel.model.PayOrderSnapshot;

import java.math.BigDecimal;

/**
 * 交易单持久化 SPI（宿主实现）。
 *
 * <p>关键契约——条件更新代替读改写：所有状态推进都通过
 * {@link #transitionState}，宿主必须实现为
 * {@code UPDATE ... SET pay_status=:to WHERE out_trade_no=:no AND pay_status=:from}
 * 式的条件更新并返回影响行数是否为 1。这是 SDK 幂等与并发安全的基石，
 * 也让编排层无须把渠道 HTTP 调用包进数据库事务。</p>
 */
public interface PayOrderRepository {

    /**
     * 创建交易单（状态 INIT），生成并回填 outTradeNo。
     *
     * @param command   支付指令
     * @param payAmount 实际请求渠道的金额（已含手续费）
     * @return 新交易单快照
     */
    PayOrderSnapshot create(PayCommand command, BigDecimal payAmount);

    /**
     * 按 (channelCode, orderNo, outTradeNo) 查交易单。
     *
     * @return 不存在返回 null
     */
    PayOrderSnapshot find(String channelCode, String orderNo, String outTradeNo);

    /**
     * 条件推进状态：仅当当前状态等于 from 时更新为 to。
     *
     * @param thirdOutTradeNo 渠道侧交易号，非 null 时一并写入
     * @return 是否更新成功（false 表示已被并发推进，调用方按幂等跳过处理）
     */
    boolean transitionState(String outTradeNo, PayStateEnum from, PayStateEnum to, String thirdOutTradeNo);

    /**
     * 退款成功后扣减可退金额并更新退款状态（部分/全额）。
     * 宿主实现应使用原子扣减（UPDATE ... SET refundable = refundable - :amount WHERE refundable >= :amount）。
     */
    void applyRefund(String outTradeNo, BigDecimal refundAmount, boolean allRefund);
}
