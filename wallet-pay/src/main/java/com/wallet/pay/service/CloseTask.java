package com.wallet.pay.service;

import com.wallet.common.lock.LockService;
import com.wallet.pay.entity.PayOrder;
import com.wallet.pay.mapper.PayOrderMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 超时关单任务：扫描已过期的 INIT/PAYING 主单，逐单持同一把锁处理。
 * 未支付→关渠道 + 补偿资产段 + 关单；渠道已支付→补单完成。
 */
@Component
public class CloseTask {

    private static final Logger log = LoggerFactory.getLogger(CloseTask.class);

    private static final int BATCH = 100;

    private final PayOrderMapper payOrderMapper;
    private final PayService payService;
    private final LockService lockService;

    public CloseTask(PayOrderMapper payOrderMapper, PayService payService, LockService lockService) {
        this.payOrderMapper = payOrderMapper;
        this.payService = payService;
        this.lockService = lockService;
    }

    @Scheduled(fixedDelay = 60000)
    public void scanExpired() {
        List<PayOrder> expired = payOrderMapper.findExpired(LocalDateTime.now(), BATCH);
        if (expired.isEmpty()) {
            return;
        }
        log.info("超时关单扫描: {} 单待处理", expired.size());
        for (PayOrder order : expired) {
            try {
                lockService.withLock(LockService.payOrderKey(order.getOrderNo()), () -> {
                    // 锁内重读，避免处理到已变化的状态
                    PayOrder fresh = payOrderMapper.findByOrderNo(order.getOrderNo());
                    if (fresh != null && ("INIT".equals(fresh.getState()) || "PAYING".equals(fresh.getState()))) {
                        payService.closeOrFinish(fresh);
                    }
                    return null;
                });
            } catch (Exception e) {
                log.error("关单处理异常, orderNo={}, err={}", order.getOrderNo(), e.getMessage());
            }
        }
    }
}
