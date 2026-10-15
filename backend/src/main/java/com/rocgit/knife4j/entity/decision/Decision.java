package com.rocgit.knife4j.entity.decision;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.rocgit.knife4j.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 决策表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("decision")
public class Decision extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("project_id")
    private Long projectId;

    private String title;

    private String content;

    @TableField("creator_id")
    private Long creatorId;

    @TableField("discussion_id")
    private Long discussionId;
}
