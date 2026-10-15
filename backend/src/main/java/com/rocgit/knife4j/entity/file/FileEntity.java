package com.rocgit.knife4j.entity.file;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.rocgit.knife4j.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文件表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("`file`")
public class FileEntity extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("project_id")
    private Long projectId;

    @TableField("stage_id")
    private Long stageId;

    @TableField("uploader_id")
    private Long uploaderId;

    private String name;

    private String url;

    private Long size;

    private String type;

    private String status;

    @TableField("reject_reason")
    private String rejectReason;

    private Integer version;

    @TableField("is_latest")
    private Integer isLatest;

    @TableField("parent_id")
    private Long parentId;

    /** T-10：文件锁（锁定人） */
    @TableField("lock_user_id")
    private Long lockUserId;

    /** T-10：锁过期时间 */
    @TableField("lock_expire_at")
    private java.time.LocalDateTime lockExpireAt;
}
