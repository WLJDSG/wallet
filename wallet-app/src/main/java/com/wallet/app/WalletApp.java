package com.wallet.app;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

import java.util.TimeZone;

/**
 * 钱包应用启动类。定时任务统一走 XXL-Job（见 XxlJobConfig），不启用 @Scheduled。
 *
 * <p><b>系统时间统一 UTC</b>：JVM 默认时区设为 UTC，业务代码的 {@code LocalDateTime.now()}
 * 与入库时间一律为 0 时区，客户端按自己的时区转换展示。</p>
 */
@SpringBootApplication(scanBasePackages = "com.wallet")
@MapperScan({"com.wallet.account.mapper", "com.wallet.pay.mapper"})
@ConfigurationPropertiesScan(basePackages = "com.wallet")
public class WalletApp {

    public static void main(String[] args) {
        // 必须在任何时间 API 使用前设置：系统时间统一 UTC
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(WalletApp.class, args);
    }
}
