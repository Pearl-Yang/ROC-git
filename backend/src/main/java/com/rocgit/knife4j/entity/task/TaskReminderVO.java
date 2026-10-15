package com.rocgit.knife4j.entity.task;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 任务提醒（前端展示）
 */
@Data
@Schema(description = "任务提醒")
public class TaskReminderVO {

    @Schema(description = "提醒ID")
    private Long id;

    @Schema(description = "任务ID")
    private Long taskId;

    @Schema(description = "任务标题")
    private String taskTitle;

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "项目名")
    private String projectName;

    @Schema(description = "提醒类型：before_due/overdue/assign")
    private String reminderType;

    @Schema(description = "提醒文案")
    private String message;

    @Schema(description = "触发时间")
    private LocalDateTime remindAt;

    @Schema(description = "是否已发送：0=未发，1=已发")
    private Integer isSent;

    @Schema(description = "已发时间")
    private LocalDateTime sentAt;
}
