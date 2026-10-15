package com.rocgit.knife4j.controller;

import com.rocgit.knife4j.common.Result;
import com.rocgit.knife4j.common.UserContext;
import com.rocgit.knife4j.dto.decision.*;
import com.rocgit.knife4j.entity.decision.DecisionVO;
import com.rocgit.knife4j.service.decision.DecisionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/decision")
@Tag(name = "09.决策", description = "决策记录")
@SecurityRequirement(name = "Bearer")
public class DecisionController {

    @Autowired
    private DecisionService decisionService;

    @Operation(summary = "决策列表")
    @GetMapping("/list")
    public Result<Map<String, Object>> list(@RequestParam Long projectId,
                                            @RequestParam(defaultValue = "1") Integer pageNum,
                                            @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.success(decisionService.list(projectId, pageNum, pageSize));
    }

    @Operation(summary = "决策详情")
    @GetMapping("/{decisionId}")
    public Result<DecisionVO> detail(@PathVariable Long decisionId) {
        return Result.success(decisionService.detail(decisionId));
    }

    @Operation(summary = "新建决策")
    @PostMapping("/create")
    public Result<DecisionVO> create(@Valid @RequestBody CreateDecisionRequest request) {
        Long uid = UserContext.requireUserId();
        return Result.success(decisionService.create(uid, request));
    }

    @Operation(summary = "更新决策")
    @PutMapping("/{decisionId}")
    public Result<Void> update(@PathVariable Long decisionId, @Valid @RequestBody UpdateDecisionRequest request) {
        decisionService.update(decisionId, request);
        return Result.success("更新成功", null);
    }

    @Operation(summary = "删除决策")
    @DeleteMapping("/{decisionId}")
    public Result<Void> delete(@PathVariable Long decisionId) {
        decisionService.delete(decisionId);
        return Result.success("删除成功", null);
    }
}
