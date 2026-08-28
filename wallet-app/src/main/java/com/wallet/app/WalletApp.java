package com.wallet.app;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * 钱包应用启动类。定时任务统一走 XXL-Job（见 XxlJobConfig），不启用 @Scheduled。
 */
@SpringBootApplication(scanBasePackages = "com.wallet")
@MapperScan({"com.wallet.account.mapper", "com.wallet.pay.mapper"})
@ConfigurationPropertiesScan(basePackages = "com.wallet")
public class WalletApp {

    public static void main(String[] args) {
        SpringApplication.run(WalletApp.class, args);
    }
}
