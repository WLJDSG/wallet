package com.wallet.app.config;

import com.wallet.common.lock.LockService;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson 客户端装配。不用 redisson-spring-boot-starter（Boot 4 适配滞后），手动装配最稳。
 */
@Configuration
public class RedisConfig {

    @Bean
    public RedissonClient redissonClient(@Value("${spring.data.redis.host:127.0.0.1}") String host,
        @Value("${spring.data.redis.port:6379}") int port) {
        Config config = new Config();
        config.useSingleServer()
            .setAddress("redis://" + host + ":" + port)
            .setConnectTimeout(3000)
            .setTimeout(5000);
        return Redisson.create(config);
    }

    /** 钱包统一分布式锁（同一支付单一把锁） */
    @Bean
    public LockService lockService(RedissonClient redissonClient) {
        return new LockService(redissonClient);
    }
}
