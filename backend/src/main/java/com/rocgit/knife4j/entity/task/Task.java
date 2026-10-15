package com.rocgit.knife4j.entity.task;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.rocgit.knife4j.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 任务表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("task")
public class Task extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("project_id")
    private Long projectId;

    @TableField("stage_id")
    private Long stageId;

    private String title;

    private String description;

    @TableField("assignee_id")
    private Long assigneeId;

    private String status;

    /** T-08：进度 0-100，100=已完成 */
    @TableField("progress")
    private Integer progress;

    private LocalDateTime deadline;

    @TableField("completed_at")
    private LocalDateTime completedAt;

    /** 优先级（仅展示用，DB 中无该列） */
    @TableField(exist = false)
    private String priority;

    /** 完成说明（仅展示用，DB 中无该列） */
    @TableField(exist = false)
    private String completionRemark;
}
