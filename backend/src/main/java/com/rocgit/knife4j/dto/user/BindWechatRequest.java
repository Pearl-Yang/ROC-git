package com.rocgit.knife4j.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 绑定微信请求（用于已有账号绑定微信）
 */
@Data
@Schema(description = "绑定微信请求")
public class BindWechatRequest {

    @NotBlank
    @Schema(description = "微信 code", requiredMode = Schema.RequiredMode.REQUIRED)
    private String code;

    @Schema(description = "微信 openid（与 code 二选一；P1-1 用于真绑定）")
    private String openid;

    @Schema(description = "微信 unionid")
    private String unionid;

    @NotBlank
    @Schema(description = "微信昵称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String nickname;

    @NotBlank
    @Schema(description = "微信头像URL", requiredMode = Schema.RequiredMode.REQUIRED)
    private String avatarUrl;
}
