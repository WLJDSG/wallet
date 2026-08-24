package com.wallet.app.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.config.Config;
import org.redisson.spring.data.connection.RedissonConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 装配：依赖走 spring-boot-starter-data-redis（排除 Lettuce），底层统一 Redisson。
 *
 * <p>RedissonClient 手动装配（不用 redisson-spring-boot-starter，Boot 4 适配滞后）；
 * {@link RedissonConnectionFactory} 把它桥接给 Spring Data Redis，
 * RedisTemplate/StringRedisTemplate 由 Boot 自动装配且底层同样走 Redisson。</p>
 *
 * <p>Redisson 原生 API（分布式锁经 lock4j 的 RedissonLockExecutor、支付密码票据/计数用
 * RBucket/RAtomicLong）的值序列化/反序列化用 Jackson JSON codec，存储内容可读、跨语言。</p>
 */
@Configuration
public class RedisConfig {

    @Bean
    public RedissonClient redissonClient(@Value("${spring.data.redis.host:127.0.0.1}") String host,
        @Value("${spring.data.redis.port:6379}") int port) {
        Config config = new Config();
        config.setCodec(new JsonJacksonCodec());
        config.useSingleServer()
            .setAddress("redis://" + host + ":" + port)
            .setConnectTimeout(3000)
            .setTimeout(5000);
        return Redisson.create(config);
    }

    /** Spring Data Redis 连接工厂：底层复用上面的 RedissonClient */
    @Bean
    public RedissonConnectionFactory redissonConnectionFactory(RedissonClient redissonClient) {
        return new RedissonConnectionFactory(redissonClient);
    }

    /**
     * 通用 RedisTemplate：key 用字符串，value/hashValue 用 Jackson JSON（带 @class 类型信息，
     * 反序列化可还原对象）。默认的 GenericJackson2JsonRedisSerializer 不支持 java.time，
     * 这里注册与 HTTP 出入参同格式的 {@link JacksonConfig#walletJavaTimeModule()}。
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedissonConnectionFactory connectionFactory) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(JacksonConfig.walletJavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        mapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance,
            ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        StringRedisSerializer keySerializer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer valueSerializer = new GenericJackson2JsonRedisSerializer(mapper);
        template.setKeySerializer(keySerializer);
        template.setHashKeySerializer(keySerializer);
        template.setValueSerializer(valueSerializer);
        template.setHashValueSerializer(valueSerializer);
        return template;
    }
}
