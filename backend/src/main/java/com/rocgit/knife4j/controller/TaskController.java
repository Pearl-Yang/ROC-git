package com.rocgit.knife4j.controller;

import com.rocgit.knife4j.common.Result;
import com.rocgit.knife4j.common.UserContext;
import com.rocgit.knife4j.dto.task.*;
import com.rocgit.knife4j.entity.task.TaskVO;
import com.rocgit.knife4j.service.task.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/task")
@Tag(name = "07.任务", description = "任务分配与管理")
@SecurityRequirement(name = "Bearer")
public class TaskController {

    @Autowired
    private TaskService taskService;

    @Operation(summary = "创建任务")
    @PostMapping("/create")
    public Result<TaskVO> create(@Valid @RequestBody CreateTaskRequest request) {
        Long uid = UserContext.requireUserId();
        return Result.success(taskService.create(uid, request));
    }

    @Operation(summary = "任务列表")
    @GetMapping("/list")
    public Result<Map<String, Object>> list(@RequestParam Long projectId,
                                            @RequestParam(required = false) Long assigneeId,
                                            @RequestParam(required = false) String status) {
        return Result.success(taskService.list(projectId, assigneeId, status));
    }

    @Operation(summary = "我的任务")
    @GetMapping("/my-tasks")
    public Result<Map<String, Object>> myTasks(@RequestParam(required = false) String status,
                                              @RequestParam(required = false) Boolean overdue,
                                              @RequestParam(defaultValue = "1") Integer pageNum,
                                              @RequestParam(defaultValue = "20") Integer pageSize) {
        Long uid = UserContext.requireUserId();
        return Result.success(taskService.myTasks(uid, status, overdue, pageNum, pageSize));
    }

    @Operation(summary = "即将到期")
    @GetMapping("/due-soon")
    public Result<List<TaskVO>> dueSoon(@RequestParam(defaultValue = "7") Integer days) {
        return Result.success(taskService.dueSoon(days));
    }

    @Operation(summary = "任务详情")
    @GetMapping("/{taskId}")
    public Result<TaskVO> detail(@PathVariable Long taskId) {
        return Result.success(taskService.detail(taskId));
    }

    @Operation(summary = "更新任务")
    @PutMapping("/{taskId}")
    public Result<Void> update(@PathVariable Long taskId, @Valid @RequestBody UpdateTaskRequest request) {
        taskService.update(taskId, request);
        return Result.success("更新成功", null);
    }

    @Operation(summary = "删除任务")
    @DeleteMapping("/{taskId}")
    public Result<Void> delete(@PathVariable Long taskId) {
        taskService.delete(taskId);
        return Result.success("删除成功", null);
    }

    @Operation(summary = "完成任务")
    @PostMapping("/{taskId}/complete")
    public Result<Void> complete(@PathVariable Long taskId, @RequestBody(required = false) @Valid CompleteTaskRequest request) {
        taskService.complete(taskId, request);
        return Result.success("任务已完成", null);
    }

    @Operation(summary = "放弃任务")
    @PostMapping("/{taskId}/abandon")
    public Result<Void> abandon(@PathVariable Long taskId, @RequestBody(required = false) @Valid AbandonTaskRequest request) {
        taskService.abandon(taskId, request);
        return Result.success("任务已放弃", null);
    }

    @Operation(summary = "转交任务")
    @PostMapping("/{taskId}/transfer")
    public Result<Void> transfer(@PathVariable Long taskId, @Valid @RequestBody TransferTaskRequest request) {
        taskService.transfer(taskId, request);
        return Result.success("转交成功", null);
    }

    @Operation(summary = "更新进度")
    @PostMapping("/{taskId}/progress")
    public Result<Void> updateProgress(@PathVariable Long taskId, @Valid @RequestBody TaskProgressRequest request) {
        taskService.updateProgress(taskId, request);
        return Result.success("进度已更新", null);
    }
}
