package com.rocgit.knife4j.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 小组表实体
 *
 * 对应表：group
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("`group`")
public class Group extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    @TableField("leader_id")
    private Long leaderId;

    @TableField("invite_code")
    private String inviteCode;

    private String status;

    /** 小组描述（仅展示用，DB 中无该列） */
    @TableField(exist = false)
    private String description;
}
