package com.wallet.security.core;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wallet.security.error.PaySecurityErrorCode;
import com.wallet.security.error.PaySecurityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 票据快照的 JSON 序列化助手。支付安全内核 自持序列化格式（纯 JSON object，无类型信息），
 * 与宿主 Redis 全局序列化策略解耦。
 *
 * <p>反序列化失败（含历史遗留的带类型信息格式）一律返回 null，调用方将其视同
 * 票据过期，绝不向上抛序列化异常。</p>
 */
public final class JsonHelper {

    private static final Logger log = LoggerFactory.getLogger(JsonHelper.class);

    private final ObjectMapper mapper;

    public JsonHelper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // BigDecimal 按原始小数位输出，保证挑战签名载荷中的金额文本稳定
        objectMapper.enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN);
        this.mapper = objectMapper;
    }

    /**
     * 序列化票据快照。
     *
     * @param value 票据对象
     * @return JSON 字符串
     * @throws PaySecurityException 序列化失败时抛出 PAY_SECURITY_UNAVAILABLE
     */
    public String toJson(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (Exception e) {
            log.error("支付安全票据序列化失败, type={}", value == null ? null : value.getClass().getName(), e);
            throw PaySecurityException.of(PaySecurityErrorCode.PAY_SECURITY_UNAVAILABLE);
        }
    }

    /**
     * 反序列化票据快照。
     *
     * @param json JSON 字符串，可为 null
     * @param type 目标票据类型
     * @return 票据对象；json 为 null 或解析失败时返回 null（视同过期）
     */
    public <T> T fromJson(String json, Class<T> type) {
        if (json == null) {
            return null;
        }
        try {
            return mapper.readValue(json, type);
        } catch (Exception e) {
            log.warn("支付安全票据解析失败按过期处理, type={}", type.getName());
            return null;
        }
    }
}
