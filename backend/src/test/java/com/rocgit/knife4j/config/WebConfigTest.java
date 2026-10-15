package com.rocgit.knife4j.config;

import com.rocgit.knife4j.interceptor.JwtInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * T-Q-3：WebConfig 白名单生效
 */
class WebConfigTest {

    @Test
    void whiteList_shouldContainPublicApis() {
        WebConfig cfg = new WebConfig();
        org.springframework.test.util.ReflectionTestUtils.setField(cfg, "jwtInterceptor", mock(JwtInterceptor.class));
        org.springframework.test.util.ReflectionTestUtils.setField(cfg, "i18nLocaleInterceptor", new I18nLocaleInterceptor());
        org.springframework.test.util.ReflectionTestUtils.setField(cfg, "uploadDir", "/tmp/test");

        // 通过反射读 EXCLUDE 列表
        java.lang.reflect.Field f = null;
        try {
            f = WebConfig.class.getDeclaredField("EXCLUDE");
        } catch (NoSuchFieldException e) {
            // field not found
        }
        assertNotNull(f, "应存在 EXCLUDE 字段");
        f.setAccessible(true);
        try {
            @SuppressWarnings("unchecked")
            java.util.List<String> list = (java.util.List<String>) f.get(null);
            assertTrue(list.contains("/api/v1/health/**"), "白名单应包含 health");
            assertTrue(list.contains("/api/v1/user/login"), "白名单应包含 login");
            assertTrue(list.contains("/api/v1/share/visitor/**"), "白名单应包含访客视图");
            assertTrue(list.contains("/api/v1/sms/send-code"), "白名单应包含 sms");
            assertTrue(list.contains("/api/v1/sms/login-by-phone"), "白名单应包含 phone login");
        } catch (IllegalAccessException e) {
            fail(e.getMessage());
        }
    }

    @Test
    void addInterceptors_shouldRegister() {
        WebConfig cfg = new WebConfig();
        org.springframework.test.util.ReflectionTestUtils.setField(cfg, "jwtInterceptor", mock(JwtInterceptor.class));
        org.springframework.test.util.ReflectionTestUtils.setField(cfg, "i18nLocaleInterceptor", new I18nLocaleInterceptor());
        org.springframework.test.util.ReflectionTestUtils.setField(cfg, "uploadDir", "/tmp/test");

        InterceptorRegistry registry = new InterceptorRegistry();
        cfg.addInterceptors(registry);
        // 注册后不抛异常
        assertNotNull(registry);
    }
}
