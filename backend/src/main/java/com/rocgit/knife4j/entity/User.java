package com.rocgit.knife4j.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户表实体
 *
 * 对应表：user（仅持久化：id / openid / nickname / avatar_url + BaseEntity 三列）
 *
 * 真实姓名 / 学号 / 手机号 / 邮箱 / 角色 / 最近登录时间 等展示属性
 * 已拆分至 {@link com.rocgit.knife4j.entity.user.ProfileVO}，
 * 不再混入本实体，避免与表结构不一致时被 MyBatis-Plus 拼成错误 SQL。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user")
public class User extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String openid;

    private String nickname;

    /** 头像 URL（DB 列：avatar_url） */
    private String avatarUrl;

    /** 手机号（DB 列：phone，T-25 手机号+验证码登录主键） */
    private String phone;

    /** P3-5：软删标记 0 正常 / 1 已注销 */
    private Integer deleted;

    /** P3-5：注销时间 */
    private java.util.Date canceledAt;

    /** P3-5：预计物理删除时间（30 天后） */
    private java.util.Date purgeAt;

    /** P3-5：注销原因（用户填） */
    private String cancelReason;
}
