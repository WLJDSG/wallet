package com.wallet.app.security;

import com.wallet.security.spi.PaySecurityKeyValueStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/** 支付安全专用纯字符串 Redis 存储，保证计数和一次性占用的原子语义。 */
@Slf4j
@Component
public class RedisSecurityStore implements PaySecurityKeyValueStore {

    private static final DefaultRedisScript<Long> INCREMENT = new DefaultRedisScript<>(
        "local v=redis.call('INCRBY',KEYS[1],ARGV[1]); if tonumber(ARGV[2])>0 then redis.call('EXPIRE',KEYS[1],ARGV[2]) end; return v",
        Long.class);

    private final StringRedisTemplate redis;

    public RedisSecurityStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override public String get(String key) { return redis.opsForValue().get(key); }

    @Override public boolean set(String key, String value, long ttlSeconds) {
        try {
            redis.opsForValue().set(key, value, Duration.ofSeconds(ttlSeconds));
            return true;
        } catch (RuntimeException e) {
            log.error("支付安全 Redis 写入失败 | key={}", key, e);
            return false;
        }
    }

    @Override public boolean setIfAbsent(String key, String value, long ttlSeconds) {
        try {
            return Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(key, value, Duration.ofSeconds(ttlSeconds)));
        } catch (RuntimeException e) {
            log.error("支付安全 Redis 原子占用失败 | key={}", key, e);
            return false;
        }
    }

    @Override public boolean delete(String key) { return Boolean.TRUE.equals(redis.delete(key)); }

    @Override public long increment(String key, long delta, long ttlSeconds) {
        Long value = redis.execute(INCREMENT, List.of(key), String.valueOf(delta), String.valueOf(ttlSeconds));
        return value == null ? 0L : value;
    }
}
