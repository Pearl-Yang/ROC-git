package com.rocgit.knife4j.service.preview;

import com.rocgit.knife4j.entity.file.FileAuditLog;
import com.rocgit.knife4j.entity.file.FileEntity;
import com.rocgit.knife4j.mapper.file.FileAuditLogMapper;
import com.rocgit.knife4j.service.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * T-12 Office → PDF 转码服务。
 *
 * <p>策略：
 * <ol>
 *   <li>优先调用系统命令 {@code libreoffice --headless --convert-to pdf}（容器内 apt 装）</li>
 *   <li>命令不可用时跳过转码、返 needs_convert，由前端走下载兜底</li>
 *   <li>同一文件并发去重；失败 3 次放弃并记审计</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileConvertService {

    private final FileStorageService fileStorageService;
    private final FileAuditLogMapper fileAuditLogMapper;

    @Value("${rocgit.preview.pdf-cache-dir:/data/files/preview}")
    private String pdfCacheDir;

    @Value("${rocgit.preview.libreoffice-bin:libreoffice}")
    private String libreofficeBin;

    @Value("${rocgit.preview.convert-timeout-seconds:90}")
    private int timeoutSec;

    @Value("${rocgit.preview.max-retries:3}")
    private int maxRetries;

    /** 已转码文件缓存（fileId → 绝对路径） */
    private final ConcurrentHashMap<Long, Path> cache = new ConcurrentHashMap<>();
    /** 转码中或失败次数 */
    private final ConcurrentHashMap<Long, Integer> failureCount = new ConcurrentHashMap<>();
    private final Set<String> libreofficeChecked = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private volatile boolean libreofficeAvailable = false;

    private final ExecutorService pool = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "file-convert-");
        t.setDaemon(true);
        return t;
    });

    @PostConstruct
    public void init() {
        try {
            Path dir = Paths.get(pdfCacheDir).toAbsolutePath();
            Files.createDirectories(dir);
            log.info("[FileConvert] PDF 缓存目录：{}", dir);
        } catch (Exception e) {
            log.warn("[FileConvert] 创建 PDF 缓存目录失败：{}", e.getMessage());
        }
        pool.submit(this::probeLibreOffice);
    }

    private void probeLibreOffice() {
        if (!libreofficeChecked.add("once")) return;
        try {
            Process p = new ProcessBuilder(libreofficeBin, "--version")
                    .redirectErrorStream(true).start();
            if (p.waitFor() == 0) {
                libreofficeAvailable = true;
                log.info("[FileConvert] libreoffice 可用：{}", libreofficeBin);
            } else {
                libreofficeAvailable = false;
                log.warn("[FileConvert] libreoffice 不可用，Office 文件预览降级为 needs_convert");
            }
        } catch (Exception e) {
            libreofficeAvailable = false;
            log.warn("[FileConvert] 探测 libreoffice 失败：{}", e.getMessage());
        }
    }

    /**
     * 检查缓存；命中且文件存在直接返回；未命中时尝试同步转码（仅 1 次快速尝试）。
     */
    public Path ensureConverted(Long fileId, FileEntity file) {
        Path cached = cache.get(fileId);
        if (cached != null && Files.exists(cached)) return cached;
        Path onDisk = pdfPath(fileId);
        if (Files.exists(onDisk) && onDisk.toFile().length() > 0) {
            cache.put(fileId, onDisk);
            return onDisk;
        }
        return null;
    }

    /**
     * 异步转码。失败时记录失败次数；超过 maxRetries 后放弃。
     */
    @Async
    public void asyncConvert(Long fileId, FileEntity file) {
        if (failureCount.getOrDefault(fileId, 0) >= maxRetries) {
            log.info("[FileConvert] fileId={} 已超过最大重试次数，跳过", fileId);
            return;
        }
        pool.submit(() -> doConvert(fileId, file));
    }

    private void doConvert(Long fileId, FileEntity file) {
        if (!libreofficeAvailable) probeLibreOffice();
        if (!libreofficeAvailable) {
            failureCount.merge(fileId, 1, Integer::sum);
            return;
        }
        Path disk = fileStorageService.resolveToDisk(file.getUrl());
        if (disk == null || !Files.exists(disk)) {
            log.warn("[FileConvert] 源文件不存在 fileId={}", fileId);
            failureCount.merge(fileId, 1, Integer::sum);
            return;
        }
        Path outDir = Paths.get(pdfCacheDir).toAbsolutePath();
        try {
            Files.createDirectories(outDir);
        } catch (Exception e) {
            log.error("[FileConvert] 创建缓存目录失败：{}", e.getMessage());
            return;
        }
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    libreofficeBin, "--headless", "--convert-to", "pdf",
                    "--outdir", outDir.toString(), disk.toString())
                    .redirectErrorStream(true);
            Process p = pb.start();
            // 消费输出，避免阻塞
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    log.debug("[libreoffice] {}", line);
                }
            }
            boolean done = p.waitFor(timeoutSec, java.util.concurrent.TimeUnit.SECONDS);
            if (!done) {
                p.destroyForcibly();
                log.warn("[FileConvert] 超时 fileId={}", fileId);
                failureCount.merge(fileId, 1, Integer::sum);
                logAudit(fileId, false, "timeout");
                return;
            }
            Path produced = outDir.resolve(stripExt(disk.getFileName().toString()) + ".pdf");
            if (Files.exists(produced) && produced.toFile().length() > 0) {
                // 规范命名为 <fileId>.pdf
                Path target = outDir.resolve(fileId + ".pdf");
                if (!target.equals(produced)) {
                    try {
                        Files.deleteIfExists(target);
                        Files.move(produced, target);
                    } catch (Exception moveErr) {
                        log.warn("[FileConvert] 重命名失败，保持原名：{}", moveErr.getMessage());
                        target = produced;
                    }
                }
                cache.put(fileId, target);
                failureCount.remove(fileId);
                logAudit(fileId, true, "ok");
                log.info("[FileConvert] fileId={} 已转码：{}", fileId, target);
            } else {
                failureCount.merge(fileId, 1, Integer::sum);
                logAudit(fileId, false, "no_output");
                log.warn("[FileConvert] 未产出 PDF fileId={}", fileId);
            }
        } catch (Exception e) {
            failureCount.merge(fileId, 1, Integer::sum);
            logAudit(fileId, false, e.getMessage());
            log.error("[FileConvert] 转码失败 fileId={}", fileId, e);
        }
    }

    private Path pdfPath(Long fileId) {
        return Paths.get(pdfCacheDir).toAbsolutePath().resolve(fileId + ".pdf");
    }

    private static String stripExt(String name) {
        int i = name.lastIndexOf('.');
        return i < 0 ? name : name.substring(0, i);
    }

    private void logAudit(Long fileId, boolean ok, String detail) {
        try {
            FileAuditLog log = new FileAuditLog();
            log.setFileId(fileId);
            log.setAction(ok ? "preview_convert_ok" : "preview_convert_fail");
            log.setReason(detail == null ? "" : detail);
            log.setAuditorId(0L);
            fileAuditLogMapper.insert(log);
        } catch (Exception ignore) {
            // 审计失败不影响主流程
        }
    }
}
