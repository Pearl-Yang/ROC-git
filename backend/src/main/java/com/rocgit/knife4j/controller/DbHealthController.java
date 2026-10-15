package com.rocgit.knife4j.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.rocgit.knife4j.entity.Group;
import com.rocgit.knife4j.entity.Project;
import com.rocgit.knife4j.entity.User;
import com.rocgit.knife4j.mapper.GroupMapper;
import com.rocgit.knife4j.mapper.ProjectMapper;
import com.rocgit.knife4j.mapper.UserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 数据库连通性验证 Controller
 *
 * 不修改原有业务 controller，仅做 MyBatis-Plus ↔ MySQL 连通性证明。
 * 部署后访问：GET /api/v1/health/db
 * 返回：{ db, userCount, groupCount, projectCount, message }
 */
@Tag(name = "数据库健康检查")
@RestController
@RequestMapping("/api/v1/health")
@RequiredArgsConstructor
public class DbHealthController {

    private final UserMapper userMapper;
    private final GroupMapper groupMapper;
    private final ProjectMapper projectMapper;

    @Operation(summary = "数据库连通性 + 表行数")
    @GetMapping("/db")
    public Map<String, Object> db() {
        Map<String, Object> result = new HashMap<>();
        try {
            long userCount = userMapper.selectCount(new QueryWrapper<User>());
            long groupCount = groupMapper.selectCount(new QueryWrapper<Group>());
            long projectCount = projectMapper.selectCount(new QueryWrapper<Project>());
            result.put("db", "ok");
            result.put("userCount", userCount);
            result.put("groupCount", groupCount);
            result.put("projectCount", projectCount);
            result.put("message", "MySQL connected, MyBatis-Plus active");
        } catch (Exception e) {
            result.put("db", "error");
            result.put("message", e.getMessage());
        }
        return result;
    }
}
