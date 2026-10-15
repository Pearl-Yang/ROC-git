package com.rocgit.knife4j.controller;

import com.rocgit.knife4j.common.Result;
import com.rocgit.knife4j.common.UserContext;
import com.rocgit.knife4j.dto.share.*;
import com.rocgit.knife4j.entity.share.*;
import com.rocgit.knife4j.service.share.ShareService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/share")
@Tag(name = "13.分享", description = "项目分享")
public class ShareController {

    @Autowired
    private ShareService shareService;

    @Operation(summary = "创建分享")
    @PostMapping("/create")
    public Result<ShareVO> create(@Valid @RequestBody ShareCreateRequest request) {
        // T-02a：用当前登录用户ID 替代写死的 0L
        Long uid = UserContext.requireUserId();
        return Result.success(shareService.create(uid, request));
    }

    @Operation(summary = "更新分享")
    @PutMapping("/{shareId}")
    public Result<Void> update(@PathVariable Long shareId, @Valid @RequestBody ShareUpdateRequest request) {
        shareService.update(shareId, request);
        return Result.success("更新成功", null);
    }

    @Operation(summary = "校验分享")
    @PostMapping("/validate")
    public Result<Map<String, Object>> validate(@Valid @RequestBody ShareValidateRequest request) {
        return Result.success(shareService.validate(request));
    }

    @Operation(summary = "删除分享")
    @DeleteMapping("/{shareId}")
    public Result<Void> delete(@PathVariable Long shareId) {
        shareService.delete(shareId);
        return Result.success("删除成功", null);
    }

    @Operation(summary = "我的分享")
    @GetMapping("/my-shares")
    public Result<List<ShareVO>> myShares() {
        Long uid = UserContext.requireUserId();
        return Result.success(shareService.myShares(uid));
    }

    @Operation(summary = "分享统计")
    @GetMapping("/{shareId}/stats")
    public Result<ShareStatsVO> stats(@PathVariable Long shareId) {
        return Result.success(shareService.stats(shareId));
    }

    @Operation(summary = "记录访问")
    @PostMapping("/{shareId}/visit")
    public Result<Void> visit(@PathVariable Long shareId, HttpServletRequest http) {
        shareService.recordVisit(shareId, http.getRemoteAddr(), http.getHeader("User-Agent"));
        return Result.success();
    }

    @Operation(summary = "访问日志")
    @GetMapping("/{shareId}/access-logs")
    public Result<List<AccessLogVO>> accessLogs(@PathVariable Long shareId) {
        return Result.success(shareService.accessLogs(shareId));
    }

    /**
     * T-02b 访客视图数据接口（无需鉴权）
     */
    @Operation(summary = "访客视图（公开，按权限裁剪）")
    @GetMapping("/visitor/{shareToken}")
    public Result<Map<String, Object>> visitorView(
            @PathVariable String shareToken,
            @RequestParam(required = false) String password,
            HttpServletRequest http) {
        Map<String, Object> data = shareService.buildVisitorView(shareToken, password);
        // T-02c：仅在视图装配成功（未抛异常）时记访问
        try {
            shareService.recordVisitByToken(shareToken, http.getRemoteAddr(), http.getHeader("User-Agent"));
        } catch (Exception ignore) { /* 不影响主流程 */ }
        return Result.success(data);
    }
}