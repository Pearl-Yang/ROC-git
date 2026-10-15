package com.rocgit.knife4j.service.file;

import com.rocgit.knife4j.dto.file.*;
import com.rocgit.knife4j.entity.file.*;
import com.rocgit.knife4j.entity.version.FileVersionVO;

import java.util.List;
import java.util.Map;

public interface FileService {


    FileVO upload(Long userId, FileUploadRequest request);

    Map<String, Object> list(Long projectId, Long stageId, String type, String status, int pageNum, int pageSize);

    FileVO detail(Long fileId);

    void delete(Long fileId);

    void rename(Long fileId, String name);

    void move(Long fileId, Long stageId);

    void batchDelete(List<Long> fileIds);

    void batchMove(List<Long> fileIds, Long stageId);

    void batchAudit(List<Long> fileIds, String action, String reason);

    void reject(Long fileId, String reason);

    void resubmit(Long fileId);

    FilePreviewVO preview(Long fileId);

    FileDownloadVO download(Long fileId, Long userId);

    List<FileVersionVO> versions(Long fileId);

    List<AuditLogVO> auditLogs(Long fileId);

    List<DownloadRecordVO> downloadRecords(Long fileId);

    FileTypeStatsVO typeStats(Long projectId);

    List<FileVO> exportFileList(Long projectId);

    // W3：文件锁三件套
    Map<String, Object> acquireLock(Long fileId, Long userId, int minutes);

    void releaseLock(Long fileId, Long userId);

    Map<String, Object> lockStatus(Long fileId);
}
