package com.rocgit.knife4j.controller;

import com.rocgit.knife4j.common.BusinessException;
import com.rocgit.knife4j.common.Result;
import com.rocgit.knife4j.common.ResultCode;
import com.rocgit.knife4j.common.UserContext;
import com.rocgit.knife4j.dto.preview.PreviewMetaVO;
import com.rocgit.knife4j.service.preview.PreviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * T-12 在线预览 Controller。
 *
 * <ul>
 *   <li>GET /api/v1/preview/{fileId} 返预览元信息（按 Content-Type 分发）</li>
 *   <li>GET /api/v1/preview/{fileId}/pdf 流出已转码的 PDF（命中文档中心用）</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/preview")
@Tag(name = "06.文件预览", description = "在线预览 · PDF/Image/Text/Office 转码")
@SecurityRequirement(name = "Bearer")
@RequiredArgsConstructor
public class PreviewController {

    private final PreviewService previewService;

    @Value("${rocgit.preview.pdf-cache-dir:/data/files/preview}")
    private String pdfCacheDir;

    @Operation(summary = "预览元信息（按 Content-Type 分发）")
    @GetMapping("/{fileId}")
    public Result<PreviewMetaVO> preview(@PathVariable Long fileId) {
        UserContext.requireUserId();
        return Result.success(previewService.preview(fileId));
    }

    @Operation(summary = "已转码 PDF 流（仅 converted=true 时有内容）")
    @GetMapping("/{fileId}/pdf")
    public void pdfStream(@PathVariable Long fileId, HttpServletResponse response) throws IOException {
        UserContext.requireUserId();
        Path pdf = Paths.get(pdfCacheDir).toAbsolutePath().resolve(fileId + ".pdf");
        if (!Files.exists(pdf)) {
            response.setStatus(404);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":404,\"message\":\"PDF 尚未生成\"}");
            return;
        }
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition",
                "inline; filename=\"" + fileId + ".pdf\"; filename*=UTF-8''" + fileId + ".pdf");
        response.setContentLengthLong(Files.size(pdf));
        try (OutputStream out = response.getOutputStream()) {
            Files.copy(pdf, out);
        }
    }
}
