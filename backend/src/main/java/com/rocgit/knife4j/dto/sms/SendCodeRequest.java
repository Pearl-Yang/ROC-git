package com.rocgit.knife4j.dto.sms;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/**
 * 发送验证码请求（T-25）
 */
@Data
@Schema(description = "发送短信验证码请求")
public class SendCodeRequest {

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Schema(description = "手机号（11位）", example = "13800000000", requiredMode = Schema.RequiredMode.REQUIRED)
    private String phone;

    @Schema(description = "用途：login（默认） / reset", example = "login")
    private String purpose = "login";
}
