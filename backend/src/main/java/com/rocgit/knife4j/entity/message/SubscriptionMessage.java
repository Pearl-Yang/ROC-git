package com.rocgit.knife4j.entity.message;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.rocgit.knife4j.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 微信订阅消息授权
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("subscription_message")
public class SubscriptionMessage extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("template_id")
    private String templateId;

    @TableField("user_id")
    private Long userId;

    private String openid;

    private Integer subscribed;

    @TableField("expires_at")
    private LocalDateTime expiresAt;
}
