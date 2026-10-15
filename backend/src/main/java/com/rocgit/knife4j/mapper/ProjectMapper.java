package com.rocgit.knife4j.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rocgit.knife4j.entity.Project;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ProjectMapper extends BaseMapper<Project> {

    /**
     * T-06：统计用户所在组的项目数。
     * 注意：project 表无 creator_id 字段；演示场景采用"用户作为小组成员所在的项目数"近似。
     * 若需要精确 creator 统计，需先在 project 表加 creator_id 字段。
     */
    @Select("SELECT COUNT(DISTINCT p.id) FROM project p " +
            "INNER JOIN group_member gm ON gm.group_id = p.group_id " +
            "WHERE gm.user_id = #{userId} AND gm.deleted = 0 AND p.deleted = 0")
    Long countProjectsByUserGroup(@Param("userId") Long userId);
}