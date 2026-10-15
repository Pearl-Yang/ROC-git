package com.rocgit.knife4j.controller;

import com.rocgit.knife4j.common.Result;
import com.rocgit.knife4j.common.UserContext;
import com.rocgit.knife4j.dto.project.*;
import com.rocgit.knife4j.entity.project.*;
import com.rocgit.knife4j.service.project.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/project")
@Tag(name = "04.项目", description = "项目管理")
@SecurityRequirement(name = "Bearer")
public class ProjectController {

    @Autowired
    private ProjectService projectService;

    @Operation(summary = "创建项目")
    @PostMapping("/create")
    public Result<ProjectDetailVO> create(@Valid @RequestBody CreateProjectRequest request) {
        Long uid = UserContext.requireUserId();
        return Result.success(projectService.create(uid, request));
    }

    @Operation(summary = "项目列表")
    @GetMapping("/list")
    public Result<List<ProjectVO>> list(@RequestParam Long groupId) {
        return Result.success(projectService.list(groupId));
    }
    @Operation(summary = "项目详情")
    @GetMapping("/{projectId}")
    public Result<ProjectDetailVO> detail(@PathVariable Long projectId) {
        return Result.success(projectService.detail(projectId));
    }

    @Operation(summary = "更新项目")
    @PutMapping("/{projectId}")
    public Result<Void> update(@PathVariable Long projectId, @Valid @RequestBody UpdateProjectRequest request) {
        projectService.update(projectId, request);
        return Result.success("更新成功", null);
    }

    @Operation(summary = "删除项目")
    @DeleteMapping("/{projectId}")
    public Result<Void> delete(@PathVariable Long projectId) {
        projectService.delete(projectId);
        return Result.success("删除成功", null);
    }

    @Operation(summary = "复制项目")
    @PostMapping("/{projectId}/copy")
    public Result<ProjectVO> copy(@PathVariable Long projectId, @Valid @RequestBody CopyProjectRequest request) {
        Long uid = UserContext.requireUserId();
        return Result.success(projectService.copy(uid, projectId, request));
    }

    @Operation(summary = "项目结项")
    @PostMapping("/{projectId}/complete")
    public Result<Void> complete(@PathVariable Long projectId) {
        projectService.complete(projectId);
        return Result.success("结项成功", null);
    }

    @Operation(summary = "项目进度")
    @GetMapping("/{projectId}/progress")
    public Result<Map<String, Object>> progress(@PathVariable Long projectId) {
        return Result.success(projectService.progress(projectId));
    }

    @Operation(summary = "项目动态")
    @GetMapping("/{projectId}/activities")
    public Result<List<ActivityVO>> activities(@PathVariable Long projectId,
                                               @RequestParam(required = false) String type,
                                               @RequestParam(defaultValue = "1") Integer pageNum,
                                               @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.success(projectService.activities(projectId, type, pageNum, pageSize));
    }

    @Operation(summary = "任务统计")
    @GetMapping("/{projectId}/task-stats")
    public Result<Map<String, Object>> taskStats(@PathVariable Long projectId) {
        return Result.success(projectService.taskStats(projectId));
    }

    @Operation(summary = "新增阶段")
    @PostMapping("/{projectId}/stage")
    public Result<StageVO> addStage(@PathVariable Long projectId, @Valid @RequestBody AddStageRequest request) {
        return Result.success(projectService.addStage(projectId, request));
    }

    @Operation(summary = "阶段列表")
    @GetMapping("/{projectId}/stage")
    public Result<List<StageVO>> stageList(@PathVariable Long projectId) {
        return Result.success(projectService.stageList(projectId));
    }

    @Operation(summary = "调整阶段顺序")
    @PutMapping("/{projectId}/stage/order")
    public Result<Void> stageOrder(@PathVariable Long projectId, @Valid @RequestBody StageOrderRequest request) {
        projectService.stageOrder(projectId, request);
        return Result.success("顺序已更新", null);
    }

    @Operation(summary = "阶段详情")
    @GetMapping("/{projectId}/stage/{stageId}")
    public Result<StageVO> stageDetail(@PathVariable Long projectId, @PathVariable Long stageId) {
        return Result.success(projectService.stageDetail(projectId, stageId));
    }

    @Operation(summary = "更新阶段")
    @PutMapping("/{projectId}/stage/{stageId}")
    public Result<Void> updateStage(@PathVariable Long projectId, @PathVariable Long stageId,
                                    @Valid @RequestBody UpdateStageRequest request) {
        projectService.updateStage(projectId, stageId, request);
        return Result.success("更新成功", null);
    }

    @Operation(summary = "删除阶段")
    @DeleteMapping("/{projectId}/stage/{stageId}")
    public Result<Void> deleteStage(@PathVariable Long projectId, @PathVariable Long stageId) {
        projectService.deleteStage(projectId, stageId);
        return Result.success("删除成功", null);
    }

    @Operation(summary = "项目模板列表")
    @GetMapping("/template/list")
    public Result<List<ProjectTemplateVO>> templateList() {
        return Result.success(projectService.templateList());
    }

    @Operation(summary = "模板详情")
    @GetMapping("/template/{templateId}")
    public Result<ProjectTemplateVO> templateDetail(@PathVariable Long templateId) {
        return Result.success(projectService.templateDetail(templateId));
    }

    @Operation(summary = "创建项目模板")
    @PostMapping("/template")
    public Result<ProjectTemplateVO> createTemplate(@Valid @RequestBody CreateTemplateRequest request) {
        Long uid = UserContext.requireUserId();
        return Result.success(projectService.createTemplate(uid, request));
    }

    @Operation(summary = "项目成员贡献度统计（T-06）")
    @GetMapping("/{projectId}/contribution-stats")
    public Result<List<ContributionStatsVO>> contributionStats(@PathVariable Long projectId) {
        return Result.success(projectService.contributionStats(projectId));
    }
}
