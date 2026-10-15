package com.rocgit.knife4j.entity.group;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.rocgit.knife4j.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 小组成员表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("group_member")
public class GroupMember extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("group_id")
    private Long groupId;

    @TableField("user_id")
    private Long userId;

    private String role;

    private String status;

    @TableField("joined_at")
    private LocalDateTime joinedAt;
}
