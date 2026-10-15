package com.rocgit.knife4j.entity.discussion;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.rocgit.knife4j.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 研讨表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("discussion")
public class Discussion extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("project_id")
    private Long projectId;

    private String topic;

    private String content;

    @TableField("meeting_time")
    private LocalDateTime meetingTime;

    @TableField("creator_id")
    private Long creatorId;
}
