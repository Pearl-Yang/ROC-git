package com.rocgit.knife4j.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.InstantDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Jackson 全局配置：让 LocalDateTime 同时支持 ISO（'T'）和空格两种格式。
 * 否则前端/curl 传 "2026-12-31 23:59:59" 会被 500。
 */
@Configuration
public class JacksonConfig {

    private static final DateTimeFormatter DT_SPACE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DT_ISO  = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter D_SPACE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter D_ISO  = DateTimeFormatter.ISO_LOCAL_DATE;

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        return builder -> {
            builder.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            builder.featuresToDisable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
            // 覆盖 LocalDateTime 反序列化，同时接受两种格式
            SimpleModule m = new SimpleModule("multi-datetime");
            m.addDeserializer(LocalDateTime.class, new FlexibleLocalDateTimeDeserializer());
            m.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DT_SPACE));
            m.addDeserializer(LocalDate.class, new FlexibleLocalDateDeserializer());
            m.addSerializer(LocalDate.class, new LocalDateSerializer(D_SPACE));
            builder.modules(m);
        };
    }

    /** 同时接受 ISO（'T'）和空格分隔的字符串 */
    public static class FlexibleLocalDateTimeDeserializer extends LocalDateTimeDeserializer {
        @Override
        public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            try {
                return super.deserialize(p, ctxt);
            } catch (Exception e) {
                try {
                    return LocalDateTime.parse(p.getValueAsString(), DT_SPACE);
                } catch (DateTimeParseException ex) {
                    return LocalDateTime.parse(p.getValueAsString(), DT_ISO);
                }
            }
        }
    }

    public static class FlexibleLocalDateDeserializer extends LocalDateDeserializer {
        @Override
        public LocalDate deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            try {
                return super.deserialize(p, ctxt);
            } catch (Exception e) {
                try {
                    return LocalDate.parse(p.getValueAsString(), D_SPACE);
                } catch (DateTimeParseException ex) {
                    return LocalDate.parse(p.getValueAsString(), D_ISO);
                }
            }
        }
    }
}
