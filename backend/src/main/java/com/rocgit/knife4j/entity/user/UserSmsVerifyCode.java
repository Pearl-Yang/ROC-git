package com.rocgit.knife4j.entity.user;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.rocgit.knife4j.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 短信验证码实体（T-25 手机号+验证码登录）
 *
 * 对应表：user_sms_verify_code
 *   - phone   手机号
 *   - code    6 位验证码
 *   - purpose 用途（login / reset）
 *   - expireAt 过期时间
 *   - used    是否已用
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_sms_verify_code")
public class UserSmsVerifyCode extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 手机号（11 位） */
    private String phone;

    /** 6 位验证码 */
    private String code;

    /** 用途：login / reset */
    private String purpose;

    /** 过期时间 */
    private LocalDateTime expireAt;

    /** 是否已用：0 否，1 是 */
    private Integer used;
}
