package com.rocgit.knife4j.common;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * T-E-2：后端极简 i18n 工具
 *
 * 用法：
 *   I18nUtil.get("user.notFound")              // 默认按 ThreadLocal locale
 *   I18nUtil.get("user.notFound", Locale.US)  // 显式指定
 *
 * 资源文件：resources/i18n/messages_{lang}.properties
 *   默认 messages.properties（中文）
 *   英文 messages_en.properties
 *
 * 线程内 Locale 由 I18nLocaleInterceptor 注入。
 */
public final class I18nUtil {

    private I18nUtil() {}

    private static final ThreadLocal<Locale> LOCALE = ThreadLocal.withInitial(() -> Locale.SIMPLIFIED_CHINESE);

    public static void setLocale(Locale locale) {
        LOCALE.set(locale == null ? Locale.SIMPLIFIED_CHINESE : locale);
    }

    public static Locale getLocale() {
        return LOCALE.get();
    }

    public static void clear() {
        LOCALE.remove();
    }

    /**
     * 按当前线程 Locale 翻译键；找不到时回退中文，再找不到回退 key 本身
     */
    public static String get(String key) {
        return get(key, getLocale());
    }

    public static String get(String key, Locale locale) {
        if (key == null) return "";
        try {
            ResourceBundle bundle = ResourceBundle.getBundle("i18n/messages", locale != null ? locale : Locale.SIMPLIFIED_CHINESE);
            if (bundle.containsKey(key)) return bundle.getString(key);
        } catch (Exception ignore) {}
        // 回退默认
        try {
            ResourceBundle fallback = ResourceBundle.getBundle("i18n/messages", Locale.SIMPLIFIED_CHINESE);
            if (fallback.containsKey(key)) return fallback.getString(key);
        } catch (Exception ignore) {}
        return key;
    }
}
