package com.rocgit.knife4j.controller;

import com.rocgit.knife4j.common.Result;
import com.rocgit.knife4j.common.UserContext;
import com.rocgit.knife4j.dto.group.*;
import com.rocgit.knife4j.entity.group.*;
import com.rocgit.knife4j.service.group.GroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/group")
@Tag(name = "03.小组", description = "小组管理")
@SecurityRequirement(name = "Bearer")
public class GroupController {

    @Autowired
    private GroupService groupService;

    @Operation(summary = "创建小组")
    @PostMapping("/create")
    public Result<GroupVO> create(@Valid @RequestBody CreateGroupRequest request) {
        Long uid = UserContext.requireUserId();
        return Result.success(groupService.create(uid, request));
    }

    @Operation(summary = "加入小组")
    @PostMapping("/join")
    public Result<Map<String, Object>> join(@Valid @RequestBody JoinGroupRequest request) {
        Long uid = UserContext.requireUserId();
        return Result.success(groupService.join(uid, request));
    }

    @Operation(summary = "校验邀请码")
    @PostMapping("/validate-invite-code")
    public Result<ValidateInviteCodeVO> validateInviteCode(@Valid @RequestBody ValidateInviteCodeRequest request) {
        return Result.success(groupService.validateInviteCode(request.getInviteCode()));
    }

    @Operation(summary = "我的小组列表")
    @GetMapping("/my-list")
    public Result<List<GroupVO>> myList() {
        Long uid = UserContext.requireUserId();
        return Result.success(groupService.myList(uid));
    }

    @Operation(summary = "小组详情")
    @GetMapping("/{groupId}")
    public Result<GroupVO> detail(@PathVariable Long groupId) {
        return Result.success(groupService.detail(groupId));
    }

    @Operation(summary = "更新小组")
    @PutMapping("/{groupId}")
    public Result<Void> update(@PathVariable Long groupId, @Valid @RequestBody UpdateGroupRequest request) {
        groupService.update(groupId, request);
        return Result.success("更新成功", null);
    }

    @Operation(summary = "退出小组")
    @PostMapping("/{groupId}/leave")
    public Result<Void> leave(@PathVariable Long groupId) {
        Long uid = UserContext.requireUserId();
        groupService.leave(uid, groupId);
        return Result.success("已退出", null);
    }

    @Operation(summary = "小组成员列表")
    @GetMapping("/{groupId}/members")
    public Result<Map<String, Object>> members(@PathVariable Long groupId,
                                               @RequestParam(defaultValue = "1") Integer pageNum,
                                               @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.success(groupService.members(groupId, pageNum, pageSize));
    }

    @Operation(summary = "修改成员备注")
    @PutMapping("/{groupId}/member/{userId}/remark")
    public Result<Void> updateRemark(@PathVariable Long groupId, @PathVariable Long userId,
                                     @Valid @RequestBody UpdateRemarkRequest request) {
        groupService.updateRemark(groupId, userId, request.getRemark());
        return Result.success("备注已更新", null);
    }

    @Operation(summary = "转让组长")
    @PostMapping("/{groupId}/transfer-leader")
    public Result<Void> transferLeader(@PathVariable Long groupId, @Valid @RequestBody TransferLeaderRequest request) {
        groupService.transferLeader(groupId, request);
        return Result.success("转让成功", null);
    }

    @Operation(summary = "重新生成邀请码")
    @PostMapping("/{groupId}/invite-code")
    public Result<RegenerateInviteCodeVO> regenerateInviteCode(@PathVariable Long groupId) {
        return Result.success(groupService.regenerateInviteCode(groupId));
    }

    @Operation(summary = "小组统计")
    @GetMapping("/{groupId}/stats")
    public Result<GroupStatsVO> stats(@PathVariable Long groupId) {
        return Result.success(groupService.stats(groupId));
    }

    @Operation(summary = "成员贡献排行")
    @GetMapping("/{groupId}/member-stats")
    public Result<List<MemberStatsVO>> memberStats(@PathVariable Long groupId) {
        return Result.success(groupService.memberStats(groupId));
    }
}
