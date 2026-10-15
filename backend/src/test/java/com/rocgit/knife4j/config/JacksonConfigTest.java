package com.rocgit.knife4j.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * T-Q-3：JacksonConfig 同时接受 ISO / 空格两种日期格式
 */
class JacksonConfigTest {

    private ObjectMapper buildMapper() {
        ObjectMapper m = new ObjectMapper();
        m.findAndRegisterModules();
        m.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        m.disable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        m.registerModule(new com.fasterxml.jackson.databind.module.SimpleModule("test-flex")
                .addDeserializer(LocalDateTime.class, new JacksonConfig.FlexibleLocalDateTimeDeserializer())
                .addDeserializer(LocalDate.class, new JacksonConfig.FlexibleLocalDateDeserializer()));
        return m;
    }

    @Test
    void parseIso_shouldWork() throws Exception {
        ObjectMapper m = buildMapper();
        LocalDateTime t = m.readValue("\"2026-12-31T23:59:59\"", LocalDateTime.class);
        assertEquals(2026, t.getYear());
        assertEquals(12, t.getMonthValue());
        assertEquals(31, t.getDayOfMonth());
    }

    @Test
    void parseSpace_shouldWork() throws Exception {
        ObjectMapper m = buildMapper();
        LocalDateTime t = m.readValue("\"2026-12-31 23:59:59\"", LocalDateTime.class);
        assertEquals(2026, t.getYear());
        assertEquals(12, t.getMonthValue());
        assertEquals(31, t.getDayOfMonth());
    }

    @Test
    void parseLocalDateSpace_shouldWork() throws Exception {
        ObjectMapper m = buildMapper();
        LocalDate d = m.readValue("\"2026-09-01\"", LocalDate.class);
        assertEquals(9, d.getMonthValue());
        assertEquals(1, d.getDayOfMonth());
    }

    @Test
    void parseLocalDateIso_shouldWork() throws Exception {
        ObjectMapper m = buildMapper();
        LocalDate d = m.readValue("\"2026-09-01\"", LocalDate.class);
        assertEquals(2026, d.getYear());
    }

    @Test
    void parseInvalid_shouldThrow() {
        ObjectMapper m = buildMapper();
        assertThrows(Exception.class, () -> m.readValue("\"not-a-date\"", LocalDateTime.class));
    }
}
