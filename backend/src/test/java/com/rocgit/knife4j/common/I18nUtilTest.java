package com.rocgit.knife4j.common;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

/**
 * T-E-2：i18n 工具 + ResultCode 本地化消息
 */
class I18nUtilTest {

    @Test
    void getChinese_shouldFallbackToChineseBundle() {
        I18nUtil.setLocale(Locale.SIMPLIFIED_CHINESE);
        String msg = I18nUtil.get("user.notFound");
        assertNotNull(msg);
        assertTrue(msg.contains("用户"), "中文应含'用户'，实际：" + msg);
    }

    @Test
    void getEnglish_shouldReadEnglishBundle() {
        I18nUtil.setLocale(Locale.ENGLISH);
        String msg = I18nUtil.get("user.notFound");
        assertNotNull(msg);
        assertTrue(msg.toLowerCase().contains("user") || msg.toLowerCase().contains("not found"),
                "英文应含 user/not found，实际：" + msg);
    }

    @Test
    void getUnknownKey_shouldReturnKey() {
        I18nUtil.setLocale(Locale.ENGLISH);
        String msg = I18nUtil.get("totally.unknown.key.xyz");
        assertEquals("totally.unknown.key.xyz", msg);
    }

    @Test
    void resultCode_localizedMessage_shouldSwitchByLocale() {
        I18nUtil.setLocale(Locale.SIMPLIFIED_CHINESE);
        String cn = ResultCode.FILE_LOCKED.getLocalizedMessage();
        I18nUtil.setLocale(Locale.ENGLISH);
        String en = ResultCode.FILE_LOCKED.getLocalizedMessage();
        assertNotEquals(cn, en, "中英文不应一致");
        assertTrue(en.toLowerCase().contains("lock"), "英文应含 lock：" + en);
    }

    @Test
    void clear_shouldResetToDefault() {
        I18nUtil.setLocale(Locale.ENGLISH);
        I18nUtil.clear();
        assertEquals(Locale.SIMPLIFIED_CHINESE, I18nUtil.getLocale());
    }
}
