package com.wallet.app.config;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

/**
 * Jackson 全局序列化配置（HTTP 出入参统一在这里定，业务字段无需逐个加 @JsonFormat）。
 *
 * <ul>
 *   <li>时间：LocalDateTime ⇄ {@value #DATE_TIME_PATTERN}，LocalDate ⇄ {@value #DATE_PATTERN}，
 *       LocalTime ⇄ {@value #TIME_PATTERN}；java.util.Date 同 {@value #DATE_TIME_PATTERN}，时区东八区；
 *       关闭时间戳输出（WRITE_DATES_AS_TIMESTAMPS）。</li>
 *   <li>枚举：出参默认输出 name()（带 @JsonValue 的按其值输出）；入参大小写不敏感
 *       （ACCEPT_CASE_INSENSITIVE_ENUMS），非法值仍然报错快速失败。</li>
 * </ul>
 *
 * <p>查询串/表单参数（不经 Jackson）的时间格式在 application.yml 的 spring.mvc.format 配置，
 * 与这里保持同一套 pattern。</p>
 */
@Configuration
public class JacksonConfig {

    public static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    public static final String DATE_PATTERN = "yyyy-MM-dd";
    public static final String TIME_PATTERN = "HH:mm:ss";

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer walletJacksonCustomizer() {
        DateTimeFormatter dateTime = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);
        DateTimeFormatter date = DateTimeFormatter.ofPattern(DATE_PATTERN);
        DateTimeFormatter time = DateTimeFormatter.ofPattern(TIME_PATTERN);
        return builder -> builder
            .simpleDateFormat(DATE_TIME_PATTERN)
            .timeZone(TimeZone.getTimeZone("Asia/Shanghai"))
            .serializers(
                new LocalDateTimeSerializer(dateTime),
                new LocalDateSerializer(date),
                new LocalTimeSerializer(time))
            .deserializers(
                new LocalDateTimeDeserializer(dateTime),
                new LocalDateDeserializer(date),
                new LocalTimeDeserializer(time))
            .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .featuresToEnable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS);
    }

    /**
     * 与 HTTP 出入参同格式的 java.time 模块，供其他 Jackson 场景复用
     * （如 RedisTemplate 的 value serializer——其默认 ObjectMapper 不带 java.time 支持）。
     */
    public static JavaTimeModule walletJavaTimeModule() {
        JavaTimeModule module = new JavaTimeModule();
        DateTimeFormatter dateTime = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);
        DateTimeFormatter date = DateTimeFormatter.ofPattern(DATE_PATTERN);
        DateTimeFormatter time = DateTimeFormatter.ofPattern(TIME_PATTERN);
        module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(dateTime));
        module.addSerializer(LocalDate.class, new LocalDateSerializer(date));
        module.addSerializer(LocalTime.class, new LocalTimeSerializer(time));
        module.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(dateTime));
        module.addDeserializer(LocalDate.class, new LocalDateDeserializer(date));
        module.addDeserializer(LocalTime.class, new LocalTimeDeserializer(time));
        return module;
    }
}
