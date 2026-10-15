package com.rocgit.knife4j.dto.version;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 版本回滚请求
 */
@Data
@Schema(description = "版本回滚请求")
public class VersionRollbackRequest {

    @NotBlank
    @Schema(description = "目标版本号（字符串如 2.0）", example = "2.0",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String targetVersion;

    @Schema(description = "回滚说明", example = "新版本有 bug，回退到 v2.0")
    private String remark;
}
