package com.rocgit.knife4j.controller;

import com.rocgit.knife4j.common.Result;
import com.rocgit.knife4j.dto.version.VersionRollbackRequest;
import com.rocgit.knife4j.entity.version.FileVersionVO;
import com.rocgit.knife4j.entity.version.VersionCompareVO;
import com.rocgit.knife4j.service.version.FileVersionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/version")
@Tag(name = "15.版本", description = "文件版本")
@SecurityRequirement(name = "Bearer")
public class VersionController {

    @Autowired
    private FileVersionService versionService;

    @Operation(summary = "版本列表")
    @GetMapping("/file/{fileId}/list")
    public Result<List<FileVersionVO>> list(@PathVariable Long fileId) {
        return Result.success(versionService.list(fileId));
    }

    @Operation(summary = "版本对比")
    @GetMapping("/file/{fileId}/compare")
    public Result<VersionCompareVO> compare(@PathVariable Long fileId,
                                            @RequestParam String from,
                                            @RequestParam String to) {
        return Result.success(versionService.compare(fileId, from, to));
    }

    @Operation(summary = "回滚到指定版本")
    @PostMapping("/file/{fileId}/rollback")
    public Result<Void> rollback(@PathVariable Long fileId, @Valid @RequestBody VersionRollbackRequest request) {
        versionService.rollback(fileId, request);
        return Result.success("回滚成功", null);
    }
}
