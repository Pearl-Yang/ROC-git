package com.rocgit.knife4j.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS（P1-1 显式声明 + B5 env 收紧）
 *  - dev（无 ALLOWED_ORIGINS）：放行 *（与原行为一致）
 *  - prod（设 ALLOWED_ORIGINS=https://a.com,https://b.com）：只放行列表内域名
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${rocgit.cors.allowed-origins:*}")
    private String allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] origins = splitOrigins(allowedOrigins);
        boolean wildcard = origins.length == 1 && "*".equals(origins[0]);

        CorsRegistration apiMapping = registry.addMapping("/api/**")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Authorization", "Content-Disposition")
                .allowCredentials(true)
                .maxAge(3600);
        applyOrigins(apiMapping, origins, wildcard);

        CorsRegistration filesMapping = registry.addMapping("/files/**")
                .allowedMethods("GET", "HEAD", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
        applyOrigins(filesMapping, origins, wildcard);
    }

    private static String[] splitOrigins(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return new String[] { "*" };
        }
        String[] parts = raw.split(",");
        for (int i = 0; i < parts.length; i++) {
            parts[i] = parts[i].trim();
            if (parts[i].isEmpty()) parts[i] = "*";
        }
        return parts;
    }

    private static void applyOrigins(CorsRegistration reg, String[] origins, boolean wildcard) {
        if (wildcard) {
            reg.allowedOriginPatterns("*");
        } else {
            reg.allowedOrigins(origins);
        }
    }
}
