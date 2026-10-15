package com.rocgit.knife4j.common;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 业务异常
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(int code) {
        this(code, "操作失败");
    }

    public BusinessException(Integer code) {
        this(code == null ? 500 : code, "操作失败");
    }

    public BusinessException(Integer code, String message) {
        this(code == null ? 500 : code, message);
    }

    public BusinessException(ResultCode resultCode) {
        // T-E-2：构造时按当前 Locale 取本地化消息（与 Result.error 保持一致）
        this(resultCode.getCode(), resultCode.getLocalizedMessage());
    }

    public BusinessException(ResultCode resultCode, String message) {
        this(resultCode.getCode(), message);
    }

    public BusinessException(String message) {
        this(500, message);
    }
}
