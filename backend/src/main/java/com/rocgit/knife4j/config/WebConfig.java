package com.rocgit.knife4j.config;

import com.rocgit.knife4j.interceptor.JwtInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;

/**
 * 注册 JWT 拦截器
 *
 *  拦截：/api/v1/** 除以下白名单外都要 token
 *    - /api/v1/health/**
 *    - /api/v1/user/login
 *    - /api/v1/config/**
 *    - /api/v1/group/validate-invite-code（可选，不强制登录）
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private JwtInterceptor jwtInterceptor;

    @Autowired
    private I18nLocaleInterceptor i18nLocaleInterceptor;

    /** T-01：上传根目录，用于 /files/ 静态资源映射 */
    @Value("${rocgit.upload.dir:/data/files}")
    private String uploadDir;

    private static final List<String> EXCLUDE = Arrays.asList(
            "/api/v1/health/**",
            "/api/v1/user/login",
            "/api/v1/config/**",
            "/api/v1/group/validate-invite-code",
            // T-02b：访客视图公开（无 token）
            "/api/v1/share/visitor/**",
            "/api/v1/share/validate",
            // T-25：手机号+验证码登录两个公开接口
            "/api/v1/sms/send-code",
            "/api/v1/sms/login-by-phone",
            "/v3/api-docs/**",
            "/doc.html",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/webjars/**",
            "/favicon.ico",
            // T-01：上传文件静态资源（仅 GET，不需 token）
            "/files/**",
            // T-18：流程图静态资源（仅 GET，不需 token）
            "/flowcharts/**",
            // P3-2：actuator 端点（由 nginx / k8s 网络层控制访问，不走 JWT）
            "/actuator/**",
            // T-12：转码后的 PDF 缓存（仅 GET，不需 token）
            "/preview/**"
    );

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // T-E-2：i18n 拦截器先跑（最优先），这样后续业务异常能用正确的 Locale 翻译
        registry.addInterceptor(i18nLocaleInterceptor)
                .addPathPatterns("/**");
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/v1/**")
                .excludePathPatterns(EXCLUDE);
    }

    /** T-01：把 /data/yyyyMM/uuid.ext 暴露为 /files/yyyyMM/uuid.ext */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = uploadDir.endsWith("/") ? "file:" + uploadDir : "file:" + uploadDir + "/";
        registry.addResourceHandler("/files/**")
                .addResourceLocations(location);
        // T-12：转码后的 PDF 缓存目录暴露为 /preview/{fileId}.pdf
        String previewDir = uploadDir.endsWith("/") ? uploadDir + "preview" : uploadDir + "/preview";
        registry.addResourceHandler("/preview/**")
                .addResourceLocations("file:" + previewDir + "/");
        // T-18：流程图静态资源（classpath:static/flowcharts）
        registry.addResourceHandler("/flowcharts/**")
                .addResourceLocations("classpath:static/flowcharts/");
    }
}