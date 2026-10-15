package com.rocgit.knife4j.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 元数据自动填充
 *
 * <p>配合 {@link com.rocgit.knife4j.common.BaseEntity} 使用：
 * <ul>
 *     <li>insert 时自动写入 createdAt / updatedAt / deleted</li>
 *     <li>update 时自动刷新 updatedAt</li>
 * </ul>
 *
 * <p>对未继承 BaseEntity 的早期实体（User / Group / Project）也会生效，
 * 只要其字段标注了 {@code @TableField(fill = ...)}。
 */
@Component
public class MybatisPlusMetaConfig implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        this.strictInsertFill(metaObject, "createdAt", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "deleted", Integer.class, 0);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
    }
}