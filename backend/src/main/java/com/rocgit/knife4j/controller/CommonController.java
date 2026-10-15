package com.rocgit.knife4j.controller;

import com.rocgit.knife4j.common.Result;
import com.rocgit.knife4j.common.UserContext;
import com.rocgit.knife4j.dto.common.FeedbackRequest;
import com.rocgit.knife4j.dto.common.UploadRequest;
import com.rocgit.knife4j.service.common.CommonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.io.File;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "00.通用", description = "通用配置 / 上传 / 反馈")
public class CommonController {

    @Autowired
    private CommonService commonService;

    @Value("${rocgit.upload.dir:/data/files}")
    private String uploadDir;

    @Operation(summary = "应用配置")
    @GetMapping("/config")
    public Result<Map<String, Object>> config() {
        return Result.success(commonService.config());
    }

    /**
     * T-01 改造：支持 multipart/form-data 真上传。
     * 表单字段：
     *   - file: MultipartFile，必填
     *   - type: file/avatar/poster，可选
     *   - projectId: 可选
     */
    @Operation(summary = "上传接口（multipart）")
    @PostMapping(value = "/upload", consumes = {"multipart/form-data", MediaType_ALL})
    public Result<Map<String, Object>> upload(
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "projectId", required = false) Long projectId) {
        UploadRequest req = new UploadRequest();
        req.setType(type);
        req.setProjectId(projectId);
        // 把 MultipartFile 透传到 Service：UploadRequest 内部用反射取 multipartFile
        // 这里直接通过包装方式传递给 service：把 controller 参数也包到 request
        try {
            java.lang.reflect.Field f = UploadRequest.class.getDeclaredField("multipartFile");
            f.setAccessible(true);
            f.set(req, file);
        } catch (Exception e) {
            // 忽略反射失败，回退到 byte[] 不命中即可
        }
        return Result.success(commonService.upload(req));
    }

    /** 兼容：纯 JSON + byte[] 旧路径 */
    @Operation(summary = "上传接口（占位 JSON）", deprecated = true)
    @PostMapping(value = "/upload", consumes = "application/json")
    public Result<Map<String, Object>> uploadJson(@Valid @RequestBody UploadRequest request) {
        return Result.success(commonService.upload(request));
    }

    @Operation(summary = "意见反馈")
    @PostMapping("/feedback")
    public Result<Void> feedback(@Valid @RequestBody FeedbackRequest request) {
        Long uid = UserContext.requireUserId();
        commonService.feedback(uid, request);
        return Result.success("反馈已收到", null);
    }

    private static final String MediaType_ALL = "*/*";
}