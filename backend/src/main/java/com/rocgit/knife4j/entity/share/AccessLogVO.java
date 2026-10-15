package com.rocgit.knife4j.entity.share;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 分享访问日志
 */
@Data
@Schema(description = "访问日志")
public class AccessLogVO {

    @Schema(description = "记录ID")
    private Long id;

    @Schema(description = "分享ID")
    private Long shareId;

    @Schema(description = "访客ID（匿名）")
    private String visitorId;

    @Schema(description = "访问时间")
    private LocalDateTime accessTime;

    @Schema(description = "访客IP")
    private String ip;

    @Schema(description = "访问设备")
    private String device;

    @Schema(description = "访问动作")
    private String action;

    @Schema(description = "访问的文件ID")
    private Long fileId;

    @Schema(description = "访问时间（别名）")
    private LocalDateTime visitedAt;

    @Schema(description = "用户代理")
    private String userAgent;
}
