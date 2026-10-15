package com.rocgit.knife4j.entity.message;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消息详情
 */
@Data
@Schema(description = "消息详情")
public class MessageVO {

    @Schema(description = "消息ID")
    private Long messageId;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "消息类型", allowableValues = {"system", "task", "mention", "announcement"})
    private String type;

    @Schema(description = "关联类型")
    private String relatedType;

    @Schema(description = "关联ID")
    private Long relatedId;

    @Schema(description = "消息标题")
    private String title;

    @Schema(description = "消息内容")
    private String content;

    @Schema(description = "是否已读")
    private Boolean read;

    @Schema(description = "是否已读（整数）")
    private Integer isRead;

    @Schema(description = "关联URL（点击跳转）")
    private String link;

    @Schema(description = "发送时间")
    private LocalDateTime createdAt;
}
