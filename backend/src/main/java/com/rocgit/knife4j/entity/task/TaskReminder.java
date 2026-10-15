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
 * 任务提醒
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("task_reminder")
public class TaskReminder extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("task_id")
    private Long taskId;

    @TableField("reminder_type")
    private String reminderType;

    @TableField("remind_at")
    private LocalDateTime remindAt;

    @TableField("is_sent")
    private Integer isSent;
}
