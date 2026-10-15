package com.rocgit.knife4j.controller;

import com.rocgit.knife4j.common.Result;
import com.rocgit.knife4j.common.UserContext;
import com.rocgit.knife4j.dto.discussion.*;
import com.rocgit.knife4j.entity.discussion.DiscussionVO;
import com.rocgit.knife4j.service.discussion.DiscussionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/discussion")
@Tag(name = "08.研讨", description = "研讨记录")
@SecurityRequirement(name = "Bearer")
public class DiscussionController {

    @Autowired
    private DiscussionService discussionService;

    @Operation(summary = "研讨列表")
    @GetMapping("/list")
    public Result<Map<String, Object>> list(@RequestParam Long projectId,
                                            @RequestParam(defaultValue = "1") Integer pageNum,
                                            @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.success(discussionService.list(projectId, pageNum, pageSize));
    }

    @Operation(summary = "研讨详情")
    @GetMapping("/{discussionId}")
    public Result<DiscussionVO> detail(@PathVariable Long discussionId) {
        return Result.success(discussionService.detail(discussionId));
    }

    @Operation(summary = "新建研讨")
    @PostMapping("/create")
    public Result<DiscussionVO> create(@Valid @RequestBody CreateDiscussionRequest request) {
        Long uid = UserContext.requireUserId();
        return Result.success(discussionService.create(uid, request));
    }

    @Operation(summary = "更新研讨")
    @PutMapping("/{discussionId}")
    public Result<Void> update(@PathVariable Long discussionId, @Valid @RequestBody UpdateDiscussionRequest request) {
        discussionService.update(discussionId, request);
        return Result.success("更新成功", null);
    }

    @Operation(summary = "删除研讨")
    @DeleteMapping("/{discussionId}")
    public Result<Void> delete(@PathVariable Long discussionId) {
        discussionService.delete(discussionId);
        return Result.success("删除成功", null);
    }
}
