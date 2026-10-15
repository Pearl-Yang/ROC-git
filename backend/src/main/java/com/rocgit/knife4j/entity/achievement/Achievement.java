package com.rocgit.knife4j.entity.achievement;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.rocgit.knife4j.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 项目成果
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("achievement")
public class Achievement extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("project_id")
    private Long projectId;

    @TableField("stage_id")
    private Long stageId;

    private String title;

    private String description;
}
