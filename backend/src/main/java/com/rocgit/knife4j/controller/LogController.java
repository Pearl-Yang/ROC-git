package com.rocgit.knife4j.controller;

import com.rocgit.knife4j.common.Result;
import com.rocgit.knife4j.common.UserContext;
import com.rocgit.knife4j.entity.log.LogStatsVO;
import com.rocgit.knife4j.entity.log.OperationLogVO;
import com.rocgit.knife4j.service.log.OperationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/log")
@Tag(name = "14.日志", description = "操作日志")
@SecurityRequirement(name = "Bearer")
public class LogController {

    @Autowired
    private OperationLogService logService;

    @Operation(summary = "项目日志")
    @GetMapping("/project/{projectId}")
    public Result<Map<String, Object>> projectLogs(@PathVariable Long projectId,
                                                   @RequestParam(required = false) String type,
                                                   @RequestParam(defaultValue = "1") Integer pageNum,
                                                   @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.success(logService.list(projectId, type, pageNum, pageSize));
    }

    @Operation(summary = "项目日志统计")
    @GetMapping("/project/{projectId}/stats")
    public Result<LogStatsVO> stats(@PathVariable Long projectId) {
        return Result.success(logService.stats(projectId));
    }

    @Operation(summary = "记录操作")
    @PostMapping("/record")
    public Result<Void> record(@RequestBody Map<String, Object> body) {
        Long uid = UserContext.requireUserId();
        Long projectId = body.get("projectId") == null ? 0L : Long.parseLong(body.get("projectId").toString());
        String action = (String) body.getOrDefault("action", "");
        String targetType = (String) body.getOrDefault("targetType", "");
        Long targetId = body.get("targetId") == null ? 0L : Long.parseLong(body.get("targetId").toString());
        String description = (String) body.getOrDefault("description", "");
        logService.record(uid, projectId, action, targetType, targetId, description);
        return Result.success();
    }
}
