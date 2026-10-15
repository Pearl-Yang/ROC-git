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
 * 分享访客
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("share_visitor")
public class ShareVisitor extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("share_id")
    private Long shareId;

    private String ip;

    @TableField("user_agent")
    private String userAgent;

    @TableField("visited_at")
    private LocalDateTime visitedAt;
}
