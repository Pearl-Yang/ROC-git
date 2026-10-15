package com.rocgit.knife4j.controller;

import com.rocgit.knife4j.common.Result;
import com.rocgit.knife4j.common.UserContext;
import com.rocgit.knife4j.dto.user.*;
import com.rocgit.knife4j.entity.user.*;
import com.rocgit.knife4j.service.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/user")
@Tag(name = "02.用户", description = "用户认证与个人信息")
@SecurityRequirement(name = "Bearer")
public class UserController {

    @Autowired
    private UserService userService;

    @Operation(summary = "微信登录", description = "通过微信 code 换取 JWT Token，首次登录会自动注册")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginRequest request,
                                  HttpServletRequest httpReq) {
        String ip = clientIp(httpReq);
        String device = request.getNickname() == null ? "unknown" : request.getNickname();
        return Result.success(userService.login(request, ip, device));
    }

    @Operation(summary = "获取个人信息")
    @GetMapping("/profile")
    public Result<ProfileVO> getProfile() {
        Long uid = UserContext.requireUserId();
        return Result.success(userService.getProfile(uid));
    }

    @Operation(summary = "更新个人信息")
    @PutMapping("/profile")
    public Result<Void> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        Long uid = UserContext.requireUserId();
        userService.updateProfile(uid, request);
        return Result.success("更新成功", null);
    }

    @Operation(summary = "登录历史")
    @GetMapping("/login-history")
    public Result<Map<String, Object>> loginHistory() {
        Long uid = UserContext.requireUserId();
        List<LoginHistoryVO> list = userService.loginHistory(uid, 10);
        Map<String, Object> data = new HashMap<>();
        data.put("total", list.size());
        data.put("list", list);
        return Result.success(data);
    }

    @Operation(summary = "退出登录", description = "前端清理 token 即可，后端无状态")
    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.success("已退出登录", null);
    }

    @Operation(summary = "刷新 Token", description = "用当前 token 换取新 token（真签发）")
    @PostMapping("/refresh-token")
    public Result<Map<String, Object>> refreshToken(@RequestBody(required = false) Map<String, String> body) {
        Long uid = UserContext.requireUserId();
        return Result.success(userService.refreshToken(uid));
    }

    @Operation(summary = "贡献统计")
    @GetMapping("/contribution-stats")
    public Result<ContributionStatsVO> contributionStats() {
        Long uid = UserContext.requireUserId();
        return Result.success(userService.contributionStats(uid));
    }

    @Operation(summary = "绑定微信", description = "将 openid 绑定到当前登录用户；openid 已被别人占用时，先解绑旧账号")
    @PostMapping("/bind-wechat")
    public Result<Void> bindWechat(@Valid @RequestBody BindWechatRequest request) {
        Long uid = UserContext.requireUserId();
        // 兼容：openid 优先，否则用 code 兜底（W3：可传 wx_real_<jscode> 触发真 jscode2session）
        String openid = (request.getOpenid() == null || request.getOpenid().isEmpty())
                ? request.getCode() : request.getOpenid();
        userService.bindWechat(uid, openid, request.getUnionid(),
                request.getNickname(), request.getAvatarUrl());
        return Result.success("绑定成功", null);
    }

    @Operation(summary = "查询微信 openid 绑定状态")
    @GetMapping("/openid-status")
    public Result<Map<String, Object>> openidStatus() {
        Long uid = UserContext.requireUserId();
        return Result.success(userService.openidStatus(uid));
    }

    @Operation(summary = "注销账号（P3-5 隐私合规）", description = "软删 + 30 天后由定时任务物理删除")
    @PostMapping("/cancel")
    public Result<Map<String, Object>> cancelAccount(@RequestBody(required = false) Map<String, String> body) {
        Long uid = UserContext.requireUserId();
        String reason = body == null ? null : body.get("reason");
        return Result.success(userService.cancelAccount(uid, reason));
    }

    @Operation(summary = "导出我的数据（P3-5 隐私合规）", description = "返回用户可见的全部个人信息 JSON")
    @GetMapping("/data-export")
    public Result<Map<String, Object>> exportData() {
        Long uid = UserContext.requireUserId();
        return Result.success(userService.exportUserData(uid));
    }

    private String clientIp(HttpServletRequest req) {
        String ip = req.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) ip = req.getRemoteAddr();
        return ip == null ? "" : ip;
    }
}
