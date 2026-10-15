package com.rocgit.knife4j.entity.announcement;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.rocgit.knife4j.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 公告（映射到 message 表，type='announcement' 视为公告）
 *
 * <p>历史原因：原 schema.sql 没有独立的 announcement 表，
 * 公告统一存储在 message 表中，type 字段标识为 'announcement'。
 * 为兼容已有 entity，类名沿用 Announcement，表名指向 message。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("message")
public class Announcement extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 接收者 id */
    @TableField("user_id")
    private Long userId;

    private String title;

    private String content;

    /** system / task / file / announcement */
    private String type;

    @TableField("related_type")
    private String relatedType;

    @TableField("related_id")
    private Long relatedId;

    @TableField("is_read")
    private Integer isRead;
}