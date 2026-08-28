package com.wallet.pay.job;

import com.wallet.common.trace.TraceIds;
import com.wallet.pay.entity.PayOrder;
import com.wallet.pay.mapper.PayOrderMapper;
import com.wallet.pay.service.PayServiceImpl;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 超时关单任务（XXL-Job 任务名 closeExpiredOrders，建议 1 分钟一次）：
 * 扫描已过期的 INIT/PAYING 主单，逐单持同一把锁处理。
 * 未支付→关渠道 + 补偿资产段 + 关单；渠道已支付→补单完成。
 */
@Slf4j
@Component
@AllArgsConstructor
public class CloseExpiredOrderJob {

    private static final int BATCH = 100;

    private final PayOrderMapper payOrderMapper;
    private final PayServiceImpl payService;


    @XxlJob("closeExpiredOrders")
    public void scanExpired() {
        TraceIds.seed();
        try {
            List<PayOrder> expired = payOrderMapper.findExpired(LocalDateTime.now(), BATCH);
            if (expired.isEmpty()) {
                return;
            }
            log.info("超时关单扫描: {} 单待处理", expired.size());
            for (PayOrder order : expired) {
                try {
                    // 持单锁 + 锁内重读在 PayService.closeExpired（@Lock4j）中完成
                    payService.closeExpired(order.getOrderNo());
                } catch (Exception e) {
                    log.error("关单处理异常, orderNo={}", order.getOrderNo(), e);
                }
            }
        } finally {
            TraceIds.clear();
        }
    }
}
