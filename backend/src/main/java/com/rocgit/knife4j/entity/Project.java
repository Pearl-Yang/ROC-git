package com.rocgit.knife4j.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 项目表实体
 *
 * 对应表：project
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("project")
public class Project extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("group_id")
    private Long groupId;

    private String name;

    private String description;

    @TableField("current_stage")
    private String currentStage;

    private Integer progress;

    private String status;

    @TableField("is_shared")
    private Integer isShared;

    @TableField("share_token")
    private String shareToken;

    @TableField("ended_at")
    private LocalDateTime endedAt;

    @TableField("shared_at")
    private LocalDateTime sharedAt;

    /** 启动时间（仅展示用） */
    @TableField(exist = false)
    private LocalDateTime startedAt;

    /** 开始日期（仅展示用） */
    @TableField(exist = false)
    private java.time.LocalDate startDate;

    /** 结束日期（仅展示用） */
    @TableField(exist = false)
    private java.time.LocalDate endDate;

    /** 创建者 ID（仅展示用） */
    @TableField(exist = false)
    private Long creatorId;
}
