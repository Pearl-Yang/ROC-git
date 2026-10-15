package com.rocgit.knife4j.entity.invite;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.rocgit.knife4j.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 邀请码
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("invite_code")
public class InviteCode extends BaseEntity {

    @TableId
    private String code;

    @TableField("group_id")
    private Long groupId;

    @TableField("expires_at")
    private LocalDateTime expiresAt;
}
