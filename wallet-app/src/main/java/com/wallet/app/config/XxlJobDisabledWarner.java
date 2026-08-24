package com.wallet.app.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * XXL-Job 未启用时的启动告警：超时关单不跑意味着 PAYING 订单不会自动关闭/补单、
 * 已扣资产不会自动补偿返还——联调可接受，生产必须开启。
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "xxl.job", name = "enabled", havingValue = "false", matchIfMissing = true)
public class XxlJobDisabledWarner {

    @PostConstruct
    public void warn() {
        log.warn("XXL-Job 未启用（xxl.job.enabled=false）：超时关单任务 closeExpiredOrders 不会运行，"
            + "超时的 PAYING 订单不会自动关闭/补单。生产环境必须部署 xxl-job-admin 并置 enabled=true");
    }
}
