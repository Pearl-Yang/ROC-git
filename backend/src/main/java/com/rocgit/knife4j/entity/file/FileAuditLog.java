package com.rocgit.knife4j.entity.file;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.rocgit.knife4j.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文件审核日志
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("file_audit_log")
public class FileAuditLog extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("file_id")
    private Long fileId;

    @TableField("auditor_id")
    private Long auditorId;

    private String action;

    private String reason;
}
