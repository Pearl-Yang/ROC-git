package com.rocgit.knife4j.controller;

import com.rocgit.knife4j.common.Result;
import com.rocgit.knife4j.common.UserContext;
import com.rocgit.knife4j.entity.announcement.AnnouncementVO;
import com.rocgit.knife4j.service.announcement.AnnouncementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/announcement")
@Tag(name = "12.公告", description = "公告")
@SecurityRequirement(name = "Bearer")
public class AnnouncementController {

    @Autowired
    private AnnouncementService announcementService;

    @Operation(summary = "公告列表")
    @GetMapping("/list")
    public Result<Map<String, Object>> list(@RequestParam(required = false) Long groupId,
                                            @RequestParam(required = false) Long projectId,
                                            @RequestParam(defaultValue = "1") Integer pageNum,
                                            @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.success(announcementService.list(groupId, projectId, pageNum, pageSize));
    }

    @Operation(summary = "新建公告")
    @PostMapping("/create")
    public Result<AnnouncementVO> create(@RequestBody Map<String, Object> body) {
        Long uid = UserContext.requireUserId();
        Long groupId = body.get("groupId") == null ? null : Long.parseLong(body.get("groupId").toString());
        Long projectId = body.get("projectId") == null ? null : Long.parseLong(body.get("projectId").toString());
        String title = (String) body.getOrDefault("title", "");
        String content = (String) body.getOrDefault("content", "");
        return Result.success(announcementService.create(uid, groupId, projectId, title, content));
    }

    @Operation(summary = "删除公告")
    @DeleteMapping("/{announcementId}")
    public Result<Void> delete(@PathVariable Long announcementId) {
        announcementService.delete(announcementId);
        return Result.success("删除成功", null);
    }
}
