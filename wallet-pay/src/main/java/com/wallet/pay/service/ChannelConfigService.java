package com.wallet.pay.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wallet.channel.enums.PayError;
import com.wallet.channel.error.ChannelException;
import com.wallet.pay.entity.ChannelConfig;
import com.wallet.pay.mapper.ChannelConfigMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 渠道配置服务：商户密钥等配置落 channel_config 表，不写死在 yml。
 *
 * <p>本地缓存 TTL {@value #CACHE_TTL_MS} 毫秒——改库后最迟半分钟全实例生效，
 * 无需重启；后台改完想立即生效可调 {@link #evict(String)}。
 * 未配置/停用的渠道在动作执行时抛 ChannelException（上层按渠道调用失败补偿）。</p>
 */
@Slf4j
@Service
@AllArgsConstructor
public class ChannelConfigService {

    private static final long CACHE_TTL_MS = 30_000;

    private static final ObjectMapper MAPPER = new ObjectMapper()
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private final ChannelConfigMapper channelConfigMapper;
    private final Map<String, Cached> cache = new ConcurrentHashMap<>();


    /** 缓存条目：config 可为 null（渠道未配置也缓存，防穿透） */
    private record Cached(ChannelConfig config, long loadedAt) {
    }

    /**
     * 取启用状态的渠道配置并反序列化为渠道自定义类型；未配置/停用/JSON 非法均抛渠道异常。
     */
    public <T> T requireEnabled(String channelCode, Class<T> type) {
        ChannelConfig config = load(channelCode);
        if (config == null || !Integer.valueOf(1).equals(config.getEnabled())) {
            throw new ChannelException(PayError.CHANNEL_INVOKE_ERROR,
                "渠道未配置或未启用: " + channelCode + "（channel_config 表）");
        }
        try {
            return MAPPER.readValue(config.getConfigJson(), type);
        } catch (Exception e) {
            log.error("渠道配置 JSON 解析失败, channelCode={}", channelCode, e);
            throw new ChannelException(PayError.CHANNEL_INVOKE_ERROR, "渠道配置格式错误: " + channelCode);
        }
    }

    /** 渠道是否已配置且启用（不抛异常的探测版） */
    public boolean isEnabled(String channelCode) {
        ChannelConfig config = load(channelCode);
        return config != null && Integer.valueOf(1).equals(config.getEnabled());
    }

    /** 清指定渠道缓存（后台改完配置调用可立即生效） */
    public void evict(String channelCode) {
        cache.remove(channelCode);
    }

    private ChannelConfig load(String channelCode) {
        long now = System.currentTimeMillis();
        Cached cached = cache.get(channelCode);
        if (cached != null && now - cached.loadedAt() < CACHE_TTL_MS) {
            return cached.config();
        }
        ChannelConfig fresh = channelConfigMapper.findByChannelCode(channelCode);
        cache.put(channelCode, new Cached(fresh, now));
        return fresh;
    }
}
