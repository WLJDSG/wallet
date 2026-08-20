package com.zbkj.paychannel.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * 日志用 JSON 序列化（失败降级为 toString，绝不因日志序列化影响主流程）。
 */
final class Jsons {

    private static final ObjectMapper MAPPER = new ObjectMapper()
        .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

    private Jsons() {
    }

    static String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            return String.valueOf(obj);
        }
    }
}
