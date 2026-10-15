package com.rocgit.knife4j.service.preview;

import com.rocgit.knife4j.common.BusinessException;
import com.rocgit.knife4j.common.ResultCode;
import com.rocgit.knife4j.dto.preview.PreviewMetaVO;
import com.rocgit.knife4j.entity.file.FileEntity;
import com.rocgit.knife4j.mapper.file.FileAuditLogMapper;
import com.rocgit.knife4j.mapper.file.FileMapper;
import com.rocgit.knife4j.service.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * T-12 文件预览服务。
 *
 * <p>职责：
 * <ol>
 *   <li>按 Content-Type 分发：image / pdf / text / drawio / convert / none</li>
 *   <li>text 类读取前 N KB（默认 256KB）作为内联文本</li>
 *   <li>docx/xlsx/pptx 检查是否已经转 PDF，已转直链，未转触发异步转码并返 needs_convert</li>
 *   <li>落审计日志：action=preview</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PreviewService {

    private static final Set<String> IMAGE_EXT = new HashSet<>(Arrays.asList(
            "png", "jpg", "jpeg", "gif", "webp", "svg", "bmp"));
    private static final Set<String> TEXT_EXT = new HashSet<>(Arrays.asList(
            "txt", "md", "csv", "json", "xml", "yml", "yaml", "log", "ini", "properties", "tsv"));
    private static final Set<String> CONVERTIBLE_EXT = new HashSet<>(Arrays.asList(
            "doc", "docx", "xls", "xlsx", "ppt", "pptx", "odt", "ods", "odp", "rtf"));
    private static final Set<String> DRAWIOS_EXT = new HashSet<>(Arrays.asList("drawio"));

    private final FileMapper fileMapper;
    private final FileAuditLogMapper fileAuditLogMapper;
    private final FileStorageService fileStorageService;
    private final FileConvertService fileConvertService;

    @Value("${rocgit.preview.max-text-bytes:262144}")
    private long maxTextBytes;

    @Value("${rocgit.preview.max-image-bytes:20971520}")
    private long maxImageBytes;

    @Value("${rocgit.preview.pdf-cache-dir:/data/files/preview}")
    private String pdfCacheDir;

    /**
     * 返回预览元信息；优先复用已存在的转码 PDF，未转则按需触发。
     */
    @Transactional
    public PreviewMetaVO preview(Long fileId) {
        FileEntity f = fileMapper.selectById(fileId);
        if (f == null) {
            throw new BusinessException(ResultCode.FILE_NOT_FOUND);
        }
        String ext = ext(f.getType(), f.getName());
        String contentType = probeContentType(ext);
        long totalSize = f.getSize() == null ? 0L : f.getSize();

        PreviewMetaVO vo = new PreviewMetaVO();
        vo.setFileId(fileId);
        vo.setName(f.getName());
        vo.setExt(ext);
        vo.setContentType(contentType);
        vo.setTotalSize(totalSize);
        vo.setUrl(f.getUrl());
        vo.setPreviewable(Boolean.FALSE);
        vo.setPreviewType("none");
        vo.setTruncated(Boolean.FALSE);
        vo.setConverted(Boolean.FALSE);

        // 1) 图片
        if (IMAGE_EXT.contains(ext)) {
            if (totalSize > maxImageBytes) {
                vo.setReason("too_large");
                return vo;
            }
            vo.setPreviewable(Boolean.TRUE);
            vo.setPreviewType("image");
            return vo;
        }

        // 2) PDF
        if ("pdf".equals(ext)) {
            vo.setPreviewable(Boolean.TRUE);
            vo.setPreviewType("pdf");
            return vo;
        }

        // 3) drawio
        if (DRAWIOS_EXT.contains(ext)) {
            vo.setPreviewable(Boolean.TRUE);
            vo.setPreviewType("drawio");
            return vo;
        }

        // 4) Office → 转 PDF
        if (CONVERTIBLE_EXT.contains(ext)) {
            Path converted = fileConvertService.ensureConverted(fileId, f);
            if (converted != null && Files.exists(converted)) {
                String convUrl = fileStorageService.getBaseUrl() + "/preview/" + fileId + ".pdf";
                vo.setPreviewable(Boolean.TRUE);
                vo.setPreviewType("pdf");
                vo.setConverted(Boolean.TRUE);
                vo.setConvertedUrl(convUrl);
                return vo;
            }
            // 没转好 → 触发异步转码
            fileConvertService.asyncConvert(fileId, f);
            vo.setReason("needs_convert");
            vo.setPreviewType("convert");
            return vo;
        }

        // 5) 文本（必须显式在 TEXT_EXT；其他如 zip 不再走 text 分支）
        if (TEXT_EXT.contains(ext)) {
            return loadText(f, vo);
        }

        // 6) 不支持
        vo.setReason("not_supported");
        return vo;
    }

    private PreviewMetaVO loadText(FileEntity f, PreviewMetaVO vo) {
        try {
            Path disk = fileStorageService.resolveToDisk(f.getUrl());
            if (disk == null || !Files.exists(disk)) {
                vo.setReason("missing");
                return vo;
            }
            byte[] bytes = Files.readAllBytes(disk);
            if (bytes.length > maxTextBytes) {
                byte[] head = Arrays.copyOfRange(bytes, 0, (int) maxTextBytes);
                vo.setContent(new String(head, StandardCharsets.UTF_8));
                vo.setTruncated(Boolean.TRUE);
            } else {
                vo.setContent(new String(bytes, StandardCharsets.UTF_8));
                vo.setTruncated(Boolean.FALSE);
            }
            vo.setPreviewable(Boolean.TRUE);
            vo.setPreviewType("text");
            return vo;
        } catch (IOException e) {
            log.warn("[Preview] 文本读取失败 fileId={} err={}", f.getId(), e.getMessage());
            vo.setReason("missing");
            return vo;
        }
    }

    private String ext(String type, String name) {
        if (type != null && !type.isEmpty()) return type.toLowerCase(Locale.ROOT);
        if (name == null) return "";
        int i = name.lastIndexOf('.');
        if (i < 0 || i == name.length() - 1) return "";
        return name.substring(i + 1).toLowerCase(Locale.ROOT);
    }

    private String probeContentType(String ext) {
        switch (ext) {
            case "png":  return "image/png";
            case "jpg":
            case "jpeg": return "image/jpeg";
            case "gif":  return "image/gif";
            case "webp": return "image/webp";
            case "svg":  return "image/svg+xml";
            case "bmp":  return "image/bmp";
            case "pdf":  return "application/pdf";
            case "json": return "application/json";
            case "xml":  return "application/xml";
            case "md":   return "text/markdown";
            case "csv":  return "text/csv";
            case "yml":
            case "yaml": return "text/yaml";
            case "log":  return "text/plain";
            case "drawio": return "application/xml";
            default:     return "text/plain";
        }
    }
}
