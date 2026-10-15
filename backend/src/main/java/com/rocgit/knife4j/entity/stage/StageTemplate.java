package com.rocgit.knife4j.entity.stage;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.rocgit.knife4j.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 阶段模板
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("stage_template")
public class StageTemplate extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String description;

    @TableField("stages_json")
    private String stagesJson;

    @TableField("created_by")
    private Long createdBy;
}
