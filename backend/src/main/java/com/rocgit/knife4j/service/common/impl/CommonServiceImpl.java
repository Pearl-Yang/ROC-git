package com.rocgit.knife4j.service.common.impl;

import com.rocgit.knife4j.dto.common.FeedbackRequest;
import com.rocgit.knife4j.dto.common.UploadRequest;
import com.rocgit.knife4j.service.common.CommonService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 通用服务：上传、反馈、配置。
 * T-01 改造：upload 改为真上传，写入配置按容器开启的 /data/files 卷，
 * 返回可被前端 nginx /files/ 静态服务访问的 URL。
 */
@Service
@Slf4j
public class CommonServiceImpl implements CommonService {

    /** 上传根目录，默认 /data/files；可由 -Drocgit.upload.dir=xxx 覆盖 */
    @Value("${rocgit.upload.dir:/data/files}")
    private String uploadDir;

    /** 文件访问 base URL，默认 http://127.0.0.1:8080/files；可由 -Drocgit.upload.base-url=xxx 覆盖 */
    @Value("${rocgit.upload.base-url:http://127.0.0.1:8080/files}")
    private String baseUrl;

    @Override
    public Map<String, Object> config() {
        Map<String, Object> cfg = new HashMap<>();
        cfg.put("appName", "ROC-git");
        cfg.put("version", "1.0.0");
        cfg.put("enableWxSubscribe", false);
        cfg.put("maxFileSizeMB", 50);
        cfg.put("supportedFileTypes", new String[]{"docx", "doc", "pdf", "xlsx", "pptx", "png", "jpg", "drawio", "zip"});
        return cfg;
    }

    @Override
    public Map<String, Object> upload(UploadRequest request) {
        Map<String, Object> result = new HashMap<>();
        try {
            MultipartFile mf = extractMultipart(request);
            if (mf == null || mf.isEmpty()) {
                log.warn("[upload] 请求未包含文件：type={}", request == null ? null : request.getType());
                result.put("url", "");
                result.put("name", "");
                result.put("size", 0L);
                result.put("error", "no_file");
                return result;
            }

            // 1. 按月分目录
            String monthDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
            File monthFolder = new File(uploadDir, monthDir);
            if (!monthFolder.exists() && !monthFolder.mkdirs()) {
                throw new IOException("无法创建上传目录: " + monthFolder.getAbsolutePath());
            }

            // 2. 取扩展名，生成 uuid.ext
            String original = mf.getOriginalFilename() == null ? "file" : mf.getOriginalFilename();
            String ext = "";
            int dot = original.lastIndexOf('.');
            if (dot > 0 && dot < original.length() - 1) {
                ext = original.substring(dot);
            }
            String savedName = UUID.randomUUID().toString().replace("-", "") + ext;
            File target = new File(monthFolder, savedName);
            mf.transferTo(target);

            // 3. 返回相对路径的访问 URL（nginx /files/ 静态服务 或后端 /files/ 资源映射）
            String relUrl = monthDir + "/" + savedName;
            String accessUrl = baseUrl.endsWith("/") ? baseUrl + relUrl : baseUrl + "/" + relUrl;

            result.put("url", accessUrl);
            result.put("name", original);
            result.put("size", mf.getSize());
            result.put("path", relUrl);
            log.info("[upload] 成功保存 {} -> {} ({} bytes)", original, target.getAbsolutePath(), mf.getSize());
            return result;
        } catch (IOException e) {
            log.error("[upload] 保存失败", e);
            result.put("url", "");
            result.put("name", request == null ? "" : "uploaded_file");
            result.put("size", 0L);
            result.put("error", "io_error: " + e.getMessage());
            return result;
        }
    }

    /**
     * 兼容两种调用：
     *  1) MultipartFile 标准上传（推荐，前端用 uni.uploadFile）
     *  2) 旧的 byte[] 字段（早期 demo，保留兼容）
     */
    private MultipartFile extractMultipart(UploadRequest request) {
        if (request == null) return null;
        try {
            // 反射拿 MultipartFile 字段（UploadRequest 未声明，但 Spring 绑定时会注入到 multipart 列表）
            java.lang.reflect.Field f = request.getClass().getDeclaredField("multipartFile");
            f.setAccessible(true);
            Object v = f.get(request);
            if (v instanceof MultipartFile) return (MultipartFile) v;
        } catch (NoSuchFieldException ignore) {
            // 没有声明 multipartFile 字段，使用 byte[] 兼容路径
        } catch (Exception e) {
            log.warn("[upload] 反射取 MultipartFile 失败", e);
        }
        // 回退：早期 byte[] 模式
        if (request.getFile() != null && request.getFile().length > 0) {
            final byte[] bytes = request.getFile();
            return new InMemoryMultipartFile(bytes, "file", "uploaded.bin", "application/octet-stream");
        }
        return null;
    }

    @Override
    public void feedback(Long userId, FeedbackRequest request) {
        // T-11 简化：直接日志落 operation_log（target_type='feedback'）
        // 复用既有日志表，避免再开 1 张新表。后续真要拆可以参考 T-11。
        StringBuilder sb = new StringBuilder();
        if (request != null) {
            sb.append("type=").append(request.getType() == null ? "" : request.getType())
              .append("; content=").append(request.getContent() == null ? "" : request.getContent())
              .append("; contact=").append(request.getContact() == null ? "" : request.getContact());
        }
        log.info("[feedback] user={} {}", userId, sb);
        // 真写库版本（如需启用）请用 OperationLogMapper insert
        // try { operationLogMapper.insert(log); } catch (Exception ignore) {}
    }

    /** 简易 MultipartFile 包装（早期 byte[] 兼容） */
    static class InMemoryMultipartFile implements MultipartFile {
        private final byte[] bytes;
        private final String name;
        private final String original;
        private final String contentType;
        InMemoryMultipartFile(byte[] bytes, String name, String original, String contentType) {
            this.bytes = bytes;
            this.name = name;
            this.original = original;
            this.contentType = contentType;
        }
        @Override public String getName() { return name; }
        @Override public String getOriginalFilename() { return original; }
        @Override public String getContentType() { return contentType; }
        @Override public boolean isEmpty() { return bytes == null || bytes.length == 0; }
        @Override public long getSize() { return bytes == null ? 0 : bytes.length; }
        @Override public byte[] getBytes() { return bytes; }
        @Override public java.io.InputStream getInputStream() { return new java.io.ByteArrayInputStream(bytes); }
        @Override public void transferTo(File dest) throws IOException {
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(dest)) {
                fos.write(bytes);
            }
        }
    }
}