package com.rocgit.knife4j.controller;

import com.rocgit.knife4j.common.Result;
import com.rocgit.knife4j.common.UserContext;
import com.rocgit.knife4j.entity.task.TaskReminderVO;
import com.rocgit.knife4j.service.task.TaskReminderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * T-09 任务提醒（提醒中心）
 */
@RestController
@RequestMapping("/api/v1/task-reminder")
@Tag(name = "11.任务提醒", description = "任务到期 / 逾期 / 新分配 提醒")
@SecurityRequirement(name = "Bearer")
public class TaskReminderController {

    @Autowired
    private TaskReminderService reminderService;

    @Operation(summary = "我的提醒列表")
    @GetMapping("/my")
    public Result<Map<String, Object>> myReminders(@RequestParam(defaultValue = "1") Integer pageNum,
                                                   @RequestParam(defaultValue = "20") Integer pageSize) {
        Long uid = UserContext.requireUserId();
        List<TaskReminderVO> list = reminderService.listMine(uid, pageNum, pageSize);
        int unsent = reminderService.countUnsentMine(uid);
        Map<String, Object> data = new HashMap<>();
        data.put("list", list);
        data.put("unsent", unsent);
        data.put("pageNum", pageNum);
        data.put("pageSize", pageSize);
        return Result.success(data);
    }

    @Operation(summary = "我的未读提醒数（用于 tabBar 角标）")
    @GetMapping("/unsent-count")
    public Result<Integer> unsentCount() {
        Long uid = UserContext.requireUserId();
        return Result.success(reminderService.countUnsentMine(uid));
    }

    /**
     * 手动触发一次扫描（开发 / 演示用）
     */
    @Operation(summary = "手动触发一次扫描（开发用）")
    @PostMapping("/scan")
    public Result<Map<String, Object>> scan() {
        int created = reminderService.scanAndCreate();
        int sent = reminderService.sendDueReminders();
        Map<String, Object> data = new HashMap<>();
        data.put("created", created);
        data.put("sent", sent);
        return Result.success(data);
    }
}
