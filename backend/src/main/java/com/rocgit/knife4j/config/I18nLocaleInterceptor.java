package com.rocgit.knife4j.config;

import com.rocgit.knife4j.common.I18nUtil;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Locale;

/**
 * T-E-2：解析 Accept-Language 头，把 Locale 注入 ThreadLocal
 *
 *   客户端可发：Accept-Language: en-US,en;q=0.9
 *   简化逻辑：若包含 "en" 开头则用 ENGLISH，否则用简体中文
 */
@Component
public class I18nLocaleInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String lang = request.getHeader("Accept-Language");
        Locale locale = parseLocale(lang);
        I18nUtil.setLocale(locale);
        // 同时让 Spring MessageSource 也能用
        LocaleContextHolder.setLocale(locale);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        I18nUtil.clear();
        LocaleContextHolder.resetLocaleContext();
    }

    private Locale parseLocale(String lang) {
        if (lang == null || lang.isEmpty()) return Locale.SIMPLIFIED_CHINESE;
        String s = lang.toLowerCase();
        if (s.contains("en")) return Locale.ENGLISH;
        return Locale.SIMPLIFIED_CHINESE;
    }
}
