package com.rocgit.knife4j.controller;

import com.rocgit.knife4j.common.Result;
import com.rocgit.knife4j.common.UserContext;
import com.rocgit.knife4j.dto.file.*;
import com.rocgit.knife4j.entity.file.*;
import com.rocgit.knife4j.entity.version.FileVersionVO;
import com.rocgit.knife4j.service.file.FileDownloadLogService;
import com.rocgit.knife4j.service.file.FileService;
import com.rocgit.knife4j.service.preview.PreviewService;
import com.rocgit.knife4j.service.share.ShareService;
import com.rocgit.knife4j.service.storage.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/file")
@Tag(name = "06.文件", description = "文件上传 / 审核 / 版本")
@SecurityRequirement(name = "Bearer")
public class FileController {

    @Autowired
    private FileService fileService;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private FileDownloadLogService downloadLogService;

    @Autowired
    private ShareService shareService;

    @Autowired
    private PreviewService previewService;

    @Operation(summary = "上传文件（multipart/form-data，T-01）")
    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public Result<FileVO> upload(@RequestPart("file") MultipartFile file,
                                 @RequestParam(required = false) Long projectId,
                                 @RequestParam(required = false) Long stageId,
                                 @RequestParam(required = false) String remark,
                                 @RequestParam(required = false) String name) {
        Long uid = UserContext.requireUserId();
        FileUploadRequest req = new FileUploadRequest();
        req.setFile(file);
        req.setName(name);
        req.setProjectId(projectId);
        req.setStageId(stageId);
        req.setRemark(remark);
        return Result.success(fileService.upload(uid, req));
    }

    @Operation(summary = "文件列表")
    @GetMapping("/list")
    public Result<Map<String, Object>> list(@RequestParam Long projectId,
                                            @RequestParam(required = false) Long stageId,
                                            @RequestParam(required = false) String type,
                                            @RequestParam(required = false) String status,
                                            @RequestParam(defaultValue = "1") Integer pageNum,
                                            @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.success(fileService.list(projectId, stageId, type, status, pageNum, pageSize));
    }

    @Operation(summary = "文件详情")
    @GetMapping("/{fileId}")
    public Result<FileVO> detail(@PathVariable Long fileId) {
        return Result.success(fileService.detail(fileId));
    }

    @Operation(summary = "删除文件")
    @DeleteMapping("/{fileId}")
    public Result<Void> delete(@PathVariable Long fileId) {
        fileService.delete(fileId);
        return Result.success("删除成功", null);
    }

    @Operation(summary = "重命名")
    @PutMapping("/{fileId}/rename")
    public Result<Void> rename(@PathVariable Long fileId, @Valid @RequestBody RenameFileRequest request) {
        fileService.rename(fileId, request.getName());
        return Result.success("重命名成功", null);
    }

    @Operation(summary = "移动文件")
    @PutMapping("/{fileId}/move")
    public Result<Void> move(@PathVariable Long fileId, @Valid @RequestBody MoveFileRequest request) {
        fileService.move(fileId, request.getStageId());
        return Result.success("移动成功", null);
    }

    @Operation(summary = "批量删除")
    @PostMapping("/batch-delete")
    public Result<Void> batchDelete(@RequestBody BatchDeleteRequest request) {
        fileService.batchDelete(request.getFileIds());
        return Result.success("批量删除成功", null);
    }

    @Operation(summary = "批量移动")
    @PostMapping("/batch-move")
    public Result<Void> batchMove(@RequestBody BatchMoveRequest request) {
        fileService.batchMove(request.getFileIds(), request.getStageId());
        return Result.success("批量移动成功", null);
    }

    @Operation(summary = "批量审核")
    @PostMapping("/batch-audit")
    public Result<Void> batchAudit(@RequestBody BatchAuditRequest request) {
        fileService.batchAudit(request.getFileIds(), request.getAction(), request.getReason());
        return Result.success("批量审核完成", null);
    }

    @Operation(summary = "驳回")
    @PostMapping("/{fileId}/reject")
    public Result<Void> reject(@PathVariable Long fileId, @Valid @RequestBody RejectFileRequest request) {
        fileService.reject(fileId, request.getReason());
        return Result.success("已驳回", null);
    }

    @Operation(summary = "重新提交")
    @PostMapping("/{fileId}/resubmit")
    public Result<Void> resubmit(@PathVariable Long fileId, @RequestBody(required = false) @Valid ResubmitFileRequest request) {
        fileService.resubmit(fileId);
        return Result.success("已重新提交", null);
    }

    @Operation(summary = "申请文件锁（W3 · T-10）", description = "返回锁状态；已被他人锁时会抛 FILE_LOCKED")
    @PostMapping("/{fileId}/lock")
    public Result<Map<String, Object>> lockFile(@PathVariable Long fileId) {
        Long uid = UserContext.requireUserId();
        return Result.success(fileService.acquireLock(fileId, uid, 5));
    }

    @Operation(summary = "释放文件锁（W3 · T-10）", description = "仅持有者可释放；非持有者会被忽略（不抛错）")
    @PostMapping("/{fileId}/unlock")
    public Result<Void> unlockFile(@PathVariable Long fileId) {
        Long uid = UserContext.requireUserId();
        fileService.releaseLock(fileId, uid);
        return Result.success("已释放", null);
    }

    @Operation(summary = "查询文件锁状态（W3 · T-10）")
    @GetMapping("/{fileId}/lock")
    public Result<Map<String, Object>> lockStatus(@PathVariable Long fileId) {
        return Result.success(fileService.lockStatus(fileId));
    }

    @Operation(summary = "预览（走 PreviewService 分发）")
    @GetMapping("/{fileId}/preview")
    public Result<com.rocgit.knife4j.entity.file.FilePreviewVO> preview(@PathVariable Long fileId) {
        com.rocgit.knife4j.dto.preview.PreviewMetaVO meta = previewService.preview(fileId);
        com.rocgit.knife4j.entity.file.FilePreviewVO vo = new com.rocgit.knife4j.entity.file.FilePreviewVO();
        vo.setFileId(meta.getFileId());
        vo.setName(meta.getName());
        vo.setUrl(meta.getUrl());
        vo.setType(meta.getExt());
        vo.setWatermark(true);
        vo.setPreviewable(meta.getPreviewable());
        vo.setPreviewType(meta.getPreviewType());
        vo.setContent(meta.getContent());
        vo.setTruncated(meta.getTruncated());
        vo.setTotalSize(meta.getTotalSize());
        vo.setReason(meta.getReason());
        return Result.success(vo);
    }

    /**
     * T-01 真正下载：直接把磁盘文件流回客户端。
     * 兼容匿名下载（分享场景），写入 file_audit_log（action=download）。
     * W2：provider=s3 时，优先 302 跳到签名 URL，节省后端带宽。
     */
    @Operation(summary = "下载文件（T-01：直读磁盘流 / W2：OSS 302）")
    @GetMapping("/{fileId}/download")
    public void download(@PathVariable Long fileId, HttpServletResponse response) throws Exception {
        Long uid = null;
        try { uid = UserContext.requireUserId(); } catch (Exception ignore) { /* 允许匿名（如分享场景） */ }
        FileVO vo = fileService.detail(fileId);
        if (vo == null || vo.getUrl() == null || vo.getUrl().isEmpty()) {
            response.setStatus(404);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":404,\"message\":\"文件不存在\"}");
            return;
        }
        // W2：OSS / S3 模式 → 302 重定向到签名 URL
        if (fileStorageService.supportsRedirect()) {
            String signed = fileStorageService.signedUrlIfRemote(vo.getUrl(), 300);
            if (signed != null) {
                response.setStatus(302);
                response.setHeader("Location", signed);
                if (uid != null) downloadLogService.record(fileId, uid, vo.getName());
                return;
            }
        }
        Path disk = fileStorageService.resolveToDisk(vo.getUrl());
        if (disk == null || !Files.exists(disk)) {
            response.setStatus(404);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":404,\"message\":\"磁盘文件不存在或已丢失\"}");
            return;
        }
        String ct = Files.probeContentType(disk);
        if (ct == null || ct.isEmpty()) ct = "application/octet-stream";
        response.setContentType(ct);
        String encoded = URLEncoder.encode(vo.getName() == null ? "file" : vo.getName(), StandardCharsets.UTF_8)
                .replace("+", "%20");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"" + encoded + "\"; filename*=UTF-8''" + encoded);
        response.setContentLengthLong(Files.size(disk));
        try (OutputStream out = response.getOutputStream();
             FileInputStream in = new FileInputStream(disk.toFile())) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            out.flush();
        }
        if (uid != null) downloadLogService.record(fileId, uid, vo.getName());
    }

    /**
     * 公开下载（无需登录，分享链接 / 静态直链使用）。
     * P1-1：必须传 shareToken，否则 401。
     */
    @Operation(summary = "公开下载（需 shareToken 鉴权）")
    @GetMapping("/public/download")
    public void publicDownload(@RequestParam Long fileId,
                                @RequestParam(required = false) String shareToken,
                                HttpServletResponse response) throws Exception {
        if (shareToken == null || shareToken.isEmpty()) {
            writeJsonError(response, 401, "缺少 shareToken");
            return;
        }
        try {
            Map<String, Object> view = shareService.buildVisitorView(shareToken, null);
            if (view == null) {
                writeJsonError(response, 401, "分享 token 无效或已过期");
                return;
            }
            // 权限校验：只有 view_with_download 才能用此接口拿真文件
            Object perm = view.get("permissionLevel");
            if (perm == null || !"view_with_download".equals(perm.toString())) {
                writeJsonError(response, 403, "当前权限不允许下载");
                return;
            }
        } catch (Exception ex) {
            writeJsonError(response, 401, "分享 token 校验失败: " + ex.getMessage());
            return;
        }
        download(fileId, response);
    }

    private void writeJsonError(HttpServletResponse response, int status, String msg) throws Exception {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        String safe = msg == null ? "" : msg.replace("\"", "'").replace("\n", " ");
        response.getWriter().write("{\"code\":" + status + ",\"message\":\"" + safe + "\"}");
    }

    @Operation(summary = "版本列表（走 file_version 真表）")
    @GetMapping("/{fileId}/versions")
    public Result<List<FileVersionVO>> versions(@PathVariable Long fileId) {
        return Result.success(fileService.versions(fileId));
    }

    @Operation(summary = "审核日志")
    @GetMapping("/{fileId}/audit-logs")
    public Result<List<AuditLogVO>> auditLogs(@PathVariable Long fileId) {
        return Result.success(fileService.auditLogs(fileId));
    }

    @Operation(summary = "下载记录")
    @GetMapping("/{fileId}/download-records")
    public Result<List<DownloadRecordVO>> downloadRecords(@PathVariable Long fileId) {
        return Result.success(fileService.downloadRecords(fileId));
    }

    @Operation(summary = "类型统计")
    @GetMapping("/type-stats")
    public Result<FileTypeStatsVO> typeStats(@RequestParam Long projectId) {
        return Result.success(fileService.typeStats(projectId));
    }

    @Operation(summary = "导出文件清单")
    @PostMapping("/export-list")
    public Result<List<FileVO>> exportList(@RequestBody ExportFileListRequest request) {
        return Result.success(fileService.exportFileList(request.getProjectId()));
    }
}
