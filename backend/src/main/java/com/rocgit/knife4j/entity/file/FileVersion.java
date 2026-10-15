package com.rocgit.knife4j.entity.file;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.rocgit.knife4j.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文件历史版本表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("file_version")
public class FileVersion extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("file_id")
    private Long fileId;

    @TableField("version_number")
    private String versionNumber;

    private String url;

    private String remark;

    @TableField("operator_id")
    private Long operatorId;
}
