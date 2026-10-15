package com.rocgit.knife4j.controller;

import com.rocgit.knife4j.common.Result;
import com.rocgit.knife4j.common.UserContext;
import com.rocgit.knife4j.dto.achievement.*;
import com.rocgit.knife4j.entity.achievement.AchievementVO;
import com.rocgit.knife4j.service.achievement.AchievementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/achievement")
@Tag(name = "11.成果", description = "项目成果")
@SecurityRequirement(name = "Bearer")
public class AchievementController {

    @Autowired
    private AchievementService achievementService;

    @Operation(summary = "成果列表")
    @GetMapping("/list")
    public Result<Map<String, Object>> list(@RequestParam Long projectId,
                                            @RequestParam(defaultValue = "1") Integer pageNum,
                                            @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.success(achievementService.list(projectId, pageNum, pageSize));
    }

    @Operation(summary = "成果详情")
    @GetMapping("/{achievementId}")
    public Result<AchievementVO> detail(@PathVariable Long achievementId) {
        return Result.success(achievementService.detail(achievementId));
    }

    @Operation(summary = "创建成果")
    @PostMapping("/create")
    public Result<AchievementVO> create(@Valid @RequestBody CreateAchievementRequest request) {
        Long uid = UserContext.requireUserId();
        return Result.success(achievementService.create(uid, request));
    }

    @Operation(summary = "更新成果")
    @PutMapping("/{achievementId}")
    public Result<Void> update(@PathVariable Long achievementId, @Valid @RequestBody UpdateAchievementRequest request) {
        achievementService.update(achievementId, request);
        return Result.success("更新成功", null);
    }

    @Operation(summary = "删除成果")
    @DeleteMapping("/{achievementId}")
    public Result<Void> delete(@PathVariable Long achievementId) {
        achievementService.delete(achievementId);
        return Result.success("删除成功", null);
    }
}
