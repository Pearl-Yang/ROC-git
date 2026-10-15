package com.rocgit.knife4j.dto.sms;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 手机号+验证码登录请求（T-25）
 */
@Data
@Schema(description = "手机号+验证码登录请求")
public class PhoneLoginRequest {

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Schema(description = "手机号（11位）", example = "13800000000", requiredMode = Schema.RequiredMode.REQUIRED)
    private String phone;

    @NotBlank(message = "验证码不能为空")
    @Size(min = 6, max = 6, message = "验证码必须为 6 位")
    @Schema(description = "6 位验证码", example = "482913", requiredMode = Schema.RequiredMode.REQUIRED)
    private String code;

    @Schema(description = "昵称（首次登录自动注册时使用，可选）", example = "新同学")
    private String nickname;

    @Schema(description = "头像URL（首次登录自动注册时使用，可选）")
    private String avatarUrl;
}
