package com.rocgit.knife4j.entity.version;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件版本历史
 */
@Data
@Schema(description = "文件版本")
public class FileVersionVO {

    @Schema(description = "版本记录ID")
    private Long versionId;

    @Schema(description = "文件ID")
    private Long fileId;

    @Schema(description = "版本号")
    private String versionNumber;

    @Schema(description = "文件URL")
    private String url;

    @Schema(description = "版本说明/备注")
    private String remark;

    @Schema(description = "操作人ID")
    private Long operatorId;

    @Schema(description = "版本号（整数）")
    private Integer version;

    @Schema(description = "版本说明")
    private String comment;

    @Schema(description = "上传者ID")
    private Long uploaderId;

    @Schema(description = "上传者昵称")
    private String uploaderName;

    @Schema(description = "文件大小")
    private Long size;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
