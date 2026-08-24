package com.wallet.app.config;

import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * XXL-Job 执行器装配。定时任务统一走 XXL-Job 调度（任务方法标 @XxlJob，如超时关单
 * {@code closeExpiredOrders}），不再使用 Spring @Scheduled。
 *
 * <p>需部署 xxl-job-admin 调度中心并在其后台创建执行器（appname 一致）与任务；
 * 本地没有调度中心时置 {@code xxl.job.enabled=false}，任务不会被触发。</p>
 */
@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "xxl.job", name = "enabled", havingValue = "true")
public class XxlJobConfig {

    @Bean
    public XxlJobSpringExecutor xxlJobExecutor(
        @Value("${xxl.job.admin-addresses}") String adminAddresses,
        @Value("${xxl.job.access-token:default_token}") String accessToken,
        @Value("${xxl.job.appname:wallet-executor}") String appname,
        @Value("${xxl.job.port:9999}") int port,
        @Value("${xxl.job.log-path:logs/xxl-job}") String logPath,
        @Value("${xxl.job.log-retention-days:30}") int logRetentionDays) {
        log.info("装配 XXL-Job 执行器, appname={}, admin={}", appname, adminAddresses);
        XxlJobSpringExecutor executor = new XxlJobSpringExecutor();
        executor.setAdminAddresses(adminAddresses);
        executor.setAccessToken(accessToken);
        executor.setAppname(appname);
        executor.setPort(port);
        executor.setLogPath(logPath);
        executor.setLogRetentionDays(logRetentionDays);
        return executor;
    }
}
