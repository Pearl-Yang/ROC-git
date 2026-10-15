package com.rocgit.knife4j.service.file;

import com.rocgit.knife4j.entity.file.FileAuditLog;
import com.rocgit.knife4j.mapper.file.FileAuditLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * T-15 文件下载流水：复用 file_audit_log（action='download'，reason 存文件名）。
 */
@Service
@RequiredArgsConstructor
public class FileDownloadLogService {

    private final FileAuditLogMapper fileAuditLogMapper;

    public void record(Long fileId, Long userId, String fileName) {
        if (fileId == null) return;
        FileAuditLog log = new FileAuditLog();
        log.setFileId(fileId);
        log.setAuditorId(userId == null ? 0L : userId);
        log.setAction("download");
        log.setReason(fileName == null ? "" : fileName);
        fileAuditLogMapper.insert(log);
    }
}
