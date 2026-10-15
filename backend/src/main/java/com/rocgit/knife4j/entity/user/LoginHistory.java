package com.rocgit.knife4j.entity.user;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.rocgit.knife4j.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 登录历史
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("login_history")
public class LoginHistory extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    private String device;

    private String ip;

    @TableField("login_time")
    private LocalDateTime loginTime;
}
