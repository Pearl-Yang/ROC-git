package com.rocgit.knife4j.dto.share;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Pattern;
import java.time.LocalDateTime;

/**
 * 更新分享权限请求
 *   - permissionLevel / expiresAt / needPassword / password 全部可选，不传则不修改
 */
@Data
@Schema(description = "更新分享权限请求")
public class ShareUpdateRequest {

    @Schema(description = "新权限级别", allowableValues = {"view_only", "view_with_preview", "view_with_download"})
    private String permissionLevel;

    @Schema(description = "新过期时间")
    private LocalDateTime expiresAt;

    @Schema(description = "是否启用密码")
    private Boolean needPassword;

    @Schema(description = "新密码（4-32 位），needPassword=true 时必传")
    @Pattern(regexp = "^$|^.{4,32}$", message = "密码长度需 4-32 位")
    private String password;
}
