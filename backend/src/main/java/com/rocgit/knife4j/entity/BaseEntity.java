package com.rocgit.knife4j.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 实体基类：统一 createdAt / updatedAt / deleted
 *
 * 22 张表都包含上述 3 列；新建实体只需 extends BaseEntity
 * 然后定义本表独有的字段即可。
 *
 * 自动填充由 {@code MybatisPlusMetaConfig} 处理：
 *   - INSERT 时填 createdAt、updatedAt = now()，deleted = 0
 *   - UPDATE 时自动更新 updatedAt
 *
 * 逻辑删除：
 *   - MyBatis-Plus 看到 @TableLogic 字段，SELECT 会自动加 deleted=0，
 *   - 删除走 updateById 时自动改 deleted=1，不会真正 DELETE 行
 */
@Data
public abstract class BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    @TableField(value = "deleted")
    private Integer deleted;
}
