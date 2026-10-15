package com.rocgit.knife4j.entity.share;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.rocgit.knife4j.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 项目分享
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("project_share")
public class ProjectShare extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("project_id")
    private Long projectId;

    @TableField("share_token")
    private String shareToken;

    @TableField("permission_level")
    private String permissionLevel;

    @TableField("view_count")
    private Integer viewCount;

    @TableField("creator_id")
    private Long creatorId;

    private String password;

    @TableField("expires_at")
    private LocalDateTime expiresAt;
}
