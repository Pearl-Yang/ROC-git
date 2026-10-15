package com.rocgit.knife4j.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * T-Q-2：ResultCode 唯一性 + 错误码规范
 */
class ResultCodeTest {

    @Test
    void allCodesShouldBeUnique() {
        ResultCode[] values = ResultCode.values();
        for (int i = 0; i < values.length; i++) {
            for (int j = i + 1; j < values.length; j++) {
                assertNotEquals(values[i].getCode(), values[j].getCode(),
                        "重复错误码 " + values[i].getCode() + "：" + values[i] + " vs " + values[j]);
            }
        }
    }

    @Test
    void successCodeShouldBeZero() {
        assertEquals(0, ResultCode.SUCCESS.getCode().intValue());
    }

    @Test
    void messagesShouldNotBeBlank() {
        for (ResultCode c : ResultCode.values()) {
            assertNotNull(c.getMessage(), "ResultCode " + c + " 缺消息");
            assertFalse(c.getMessage().trim().isEmpty(), "ResultCode " + c + " 消息空白");
        }
    }
}
