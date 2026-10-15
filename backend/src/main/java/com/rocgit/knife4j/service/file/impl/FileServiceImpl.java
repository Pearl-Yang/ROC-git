package com.rocgit.knife4j.service.file.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.rocgit.knife4j.common.BusinessException;
import com.rocgit.knife4j.common.ResultCode;
import com.rocgit.knife4j.dto.file.*;
import com.rocgit.knife4j.entity.file.*;
import com.rocgit.knife4j.entity.version.FileVersionVO;
import com.rocgit.knife4j.mapper.file.FileAuditLogMapper;
import com.rocgit.knife4j.mapper.file.FileMapper;
import com.rocgit.knife4j.mapper.file.FileVersionMapper;
import com.rocgit.knife4j.entity.file.FileVersion;
import com.rocgit.knife4j.service.file.FileService;
import com.rocgit.knife4j.service.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final FileMapper fileMapper;
    private final FileAuditLogMapper fileAuditLogMapper;
    private final FileVersionMapper fileVersionMapper;
    private final FileStorageService fileStorageService;

    /**
     * T-01 真上传：multipart/form-data → 落盘 → 写 file 行。
     * 同 project + 同 stage + 同 name 视为版本演进，自动 version+1 且插入 file_version。
     */
    @Override
    @Transactional
    public FileVO upload(Long userId, FileUploadRequest req) {
        MultipartFile mf = req == null ? null : req.getFile();
        boolean hasMultipart = mf != null && !mf.isEmpty();
        if (!hasMultipart && (req == null || req.getUrl() == null || req.getUrl().isEmpty())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "请选择要上传的文件");
        }

        Long projectId = req == null ? null : req.getProjectId();
        Long stageId = (req == null || req.getStageId() == null) ? 0L : req.getStageId();

        String url;
        long size;
        String ext;
        String origName;

        if (hasMultipart) {
            // 真上传：落盘
            FileStorageService.StoredFile stored = fileStorageService.store(mf, projectId);
            url = stored.getUrl();
            size = stored.getSize();
            ext = stored.getExt();
            origName = mf.getOriginalFilename() == null
                    ? (req.getName() == null ? "未命名文件" : req.getName())
                    : mf.getOriginalFilename();
        } else {
            // 兼容旧 JSON 模式（直传 URL）
            url = req.getUrl();
            size = req.getSize() == null ? 0L : req.getSize();
            ext = req.getType() == null ? "" : req.getType();
            origName = req.getName() == null ? "未命名文件" : req.getName();
        }

        // 找同 project + 同 stage + 同 name 的"上一版本" → 自动 +1
        FileEntity prev = findLatestByName(projectId, stageId, origName);

        // T-10：检查文件锁（防并发上传同名）
        if (prev != null) {
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            if (prev.getLockUserId() != null && prev.getLockUserId() > 0
                    && !prev.getLockUserId().equals(userId)
                    && prev.getLockExpireAt() != null
                    && prev.getLockExpireAt().isAfter(now)) {
                throw new BusinessException(ResultCode.FILE_LOCKED.getCode(),
                        "文件《" + origName + "》正被用户 #" + prev.getLockUserId() + " 编辑中，锁到期：" + prev.getLockExpireAt());
            }
        }
        int nextVer = prev == null ? 1 : (prev.getVersion() == null ? 1 : prev.getVersion() + 1);

        FileEntity f = new FileEntity();
        f.setProjectId(projectId);
        f.setStageId(stageId);
        f.setUploaderId(userId);
        f.setName(origName);
        f.setUrl(url);
        f.setSize(size);
        f.setType(ext);
        f.setStatus("pending");
        f.setVersion(nextVer);
        f.setIsLatest(1);
        f.setParentId(prev == null ? 0L : prev.getId());
        // T-10：上锁（5 分钟）
        f.setLockUserId(userId == null ? 0L : userId);
        f.setLockExpireAt(java.time.LocalDateTime.now().plusMinutes(5));
        fileMapper.insert(f);

        if (prev != null) {
            prev.setIsLatest(0);
            fileMapper.updateById(prev);
            // T-10：旧版本释放锁
            prev.setLockUserId(0L);
            prev.setLockExpireAt(null);
            fileMapper.updateById(prev);
        }

        // F-050 自动版本记录
        FileVersion fv = new FileVersion();
        fv.setFileId(f.getId());
        fv.setVersionNumber(nextVer + ".0");
        fv.setUrl(url);
        fv.setRemark(prev == null
                ? "首次上传"
                : "版本 " + nextVer + (req.getRemark() == null ? "" : "：" + req.getRemark()));
        fv.setOperatorId(userId == null ? 0L : userId);
        fileVersionMapper.insert(fv);

        return toFileVO(f);
    }

    private FileEntity findLatestByName(Long projectId, Long stageId, String name) {
        if (projectId == null || name == null) return null;
        QueryWrapper<FileEntity> qw = new QueryWrapper<>();
        qw.eq("project_id", projectId)
          .eq("name", name)
          .eq("is_latest", 1)
          .orderByDesc("version")
          .last("LIMIT 1");
        return fileMapper.selectOne(qw);
    }

    @Override
    public Map<String, Object> list(Long projectId, Long stageId, String type, String status, int pageNum, int pageSize) {
        QueryWrapper<com.rocgit.knife4j.entity.file.FileEntity> qw = new QueryWrapper<>();
        qw.eq("project_id", projectId);
        if (stageId != null) qw.eq("stage_id", stageId);
        if (type != null && !type.isEmpty()) qw.eq("type", type);
        if (status != null && !status.isEmpty()) qw.eq("status", status);
        qw.orderByDesc("created_at");
        List<com.rocgit.knife4j.entity.file.FileEntity> list = fileMapper.selectList(qw);
        List<FileVO> vos = list.stream().map(this::toFileVO).collect(Collectors.toList());
        Map<String, Object> data = new HashMap<>();
        data.put("total", vos.size());
        data.put("files", vos);
        return data;
    }

    @Override
    public FileVO detail(Long fileId) {
        com.rocgit.knife4j.entity.file.FileEntity f = fileMapper.selectById(fileId);
        if (f == null) return null;
        return toFileVO(f);
    }

    @Override
    @Transactional
    public void delete(Long fileId) {
        fileMapper.deleteById(fileId);
    }

    @Override
    @Transactional
    public void rename(Long fileId, String newName) {
        com.rocgit.knife4j.entity.file.FileEntity f = fileMapper.selectById(fileId);
        if (f != null) {
            f.setName(newName);
            fileMapper.updateById(f);
        }
    }

    @Override
    @Transactional
    public void move(Long fileId, Long stageId) {
        com.rocgit.knife4j.entity.file.FileEntity f = fileMapper.selectById(fileId);
        if (f != null) {
            f.setStageId(stageId);
            fileMapper.updateById(f);
        }
    }

    @Override
    @Transactional
    public void batchDelete(List<Long> fileIds) {
        fileMapper.deleteBatchIds(fileIds);
    }

    @Override
    @Transactional
    public void batchMove(List<Long> fileIds, Long stageId) {
        for (Long id : fileIds) move(id, stageId);
    }

    @Override
    @Transactional
    public void batchAudit(List<Long> fileIds, String action, String reason) {
        for (Long id : fileIds) {
            com.rocgit.knife4j.entity.file.FileEntity f = fileMapper.selectById(id);
            if (f != null) {
                f.setStatus("approve".equals(action) ? "approved" : "rejected");
                if ("reject".equals(action)) f.setRejectReason(reason);
                fileMapper.updateById(f);
            }
        }
    }

    @Override
    @Transactional
    public void reject(Long fileId, String reason) {
        com.rocgit.knife4j.entity.file.FileEntity f = fileMapper.selectById(fileId);
        if (f != null) {
            f.setStatus("rejected");
            f.setRejectReason(reason);
            fileMapper.updateById(f);
        }
    }

    @Override
    @Transactional
    public void resubmit(Long fileId) {
        com.rocgit.knife4j.entity.file.FileEntity f = fileMapper.selectById(fileId);
        if (f != null) {
            f.setStatus("pending");
            f.setRejectReason("");
            fileMapper.updateById(f);
        }
    }

    @Override
    public FilePreviewVO preview(Long fileId) {
        com.rocgit.knife4j.entity.file.FileEntity f = fileMapper.selectById(fileId);
        FilePreviewVO vo = new FilePreviewVO();
        if (f != null) {
            vo.setFileId(f.getId());
            vo.setName(f.getName());
            vo.setUrl(f.getUrl());
            vo.setType(f.getType());
            vo.setWatermark(true);
        }
        return vo;
    }

    @Override
    public FileDownloadVO download(Long fileId, Long userId) {
        com.rocgit.knife4j.entity.file.FileEntity f = fileMapper.selectById(fileId);
        FileDownloadVO vo = new FileDownloadVO();
        if (f != null) {
            vo.setFileId(f.getId());
            vo.setName(f.getName());
            vo.setUrl(f.getUrl());
            vo.setDownloadUrl(f.getUrl());
            vo.setSize(f.getSize());
            vo.setFilename(f.getName());
            vo.setExpiresAt(System.currentTimeMillis() + 3600_000);
        }
        return vo;
    }

    @Override
    public List<FileVersionVO> versions(Long fileId) {
        // T-03a：走真表 file_version
        List<FileVersion> list = fileVersionMapper.selectList(
                new QueryWrapper<FileVersion>().eq("file_id", fileId).eq("deleted", 0).orderByDesc("created_at"));
        List<FileVersionVO> result = new ArrayList<>();
        for (FileVersion v : list) {
            FileVersionVO vo = new FileVersionVO();
            vo.setVersionId(v.getId());
            vo.setFileId(v.getFileId());
            vo.setVersionNumber(v.getVersionNumber());
            vo.setUrl(v.getUrl());
            vo.setRemark(v.getRemark());
            vo.setOperatorId(v.getOperatorId());
            vo.setCreatedAt(v.getCreatedAt());
            result.add(vo);
        }
        return result;
    }

    @Override
    public List<AuditLogVO> auditLogs(Long fileId) {
        List<FileAuditLog> list = fileAuditLogMapper.selectList(
                new QueryWrapper<FileAuditLog>().eq("file_id", fileId).orderByDesc("created_at"));
        return list.stream().map(a -> {
            AuditLogVO vo = new AuditLogVO();
            vo.setLogId(a.getId());
            vo.setAction(a.getAction());
            vo.setReason(a.getReason());
            vo.setAuditorId(a.getAuditorId());
            vo.setCreatedAt(a.getCreatedAt());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public List<DownloadRecordVO> downloadRecords(Long fileId) {
        QueryWrapper<FileAuditLog> qw = new QueryWrapper<FileAuditLog>()
                .eq("file_id", fileId)
                .eq("action", "download")
                .orderByDesc("created_at")
                .last("LIMIT 100");
        return fileAuditLogMapper.selectList(qw).stream().map(a -> {
            DownloadRecordVO vo = new DownloadRecordVO();
            vo.setId(a.getId());
            vo.setUserId(a.getAuditorId());
            vo.setDownloadedAt(a.getCreatedAt());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public FileTypeStatsVO typeStats(Long projectId) {
        // T-06 顺手扩展：用真 SQL 统计 file 表里 type 分类
        FileTypeStatsVO vo = new FileTypeStatsVO();
        long document = 0, image = 0, video = 0, other = 0;
        List<com.rocgit.knife4j.entity.file.FileEntity> all = fileMapper.selectList(
                new QueryWrapper<com.rocgit.knife4j.entity.file.FileEntity>().eq("project_id", projectId).eq("deleted", 0));
        for (com.rocgit.knife4j.entity.file.FileEntity f : all) {
            String t = f.getType() == null ? "" : f.getType().toLowerCase();
            if (t.matches("docx?|xlsx?|pptx?|pdf|drawio|zip")) document++;
            else if (t.matches("png|jpe?g|gif|bmp|webp|svg")) image++;
            else if (t.matches("mp4|m4v|mov|avi|wmv")) video++;
            else other++;
        }
        vo.setDocument((int) document);
        vo.setImage((int) image);
        vo.setVideo((int) video);
        vo.setOther((int) other);
        vo.setTotal((int) (document + image + video + other));
        return vo;
    }

    @Override
    public List<FileVO> exportFileList(Long projectId) {
        List<com.rocgit.knife4j.entity.file.FileEntity> list = fileMapper.selectList(
                new QueryWrapper<com.rocgit.knife4j.entity.file.FileEntity>().eq("project_id", projectId));
        return list.stream().map(this::toFileVO).collect(Collectors.toList());
    }

    // ====================== W3 · T-10 文件锁 ======================

    @Override
    @Transactional
    public Map<String, Object> acquireLock(Long fileId, Long userId, int minutes) {
        com.rocgit.knife4j.entity.file.FileEntity f = fileMapper.selectById(fileId);
        if (f == null) throw new BusinessException(ResultCode.FILE_NOT_FOUND);
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        Long cur = f.getLockUserId() == null ? 0L : f.getLockUserId();
        java.time.LocalDateTime expire = f.getLockExpireAt();
        boolean held = cur > 0 && expire != null && expire.isAfter(now) && !cur.equals(userId);
        if (held) {
            throw new BusinessException(ResultCode.FILE_LOCKED,
                    "文件正被用户 #" + cur + " 编辑中，锁到期：" + expire);
        }
        java.time.LocalDateTime newExpire = now.plusMinutes(minutes <= 0 ? 5 : minutes);
        f.setLockUserId(userId == null ? 0L : userId);
        f.setLockExpireAt(newExpire);
        fileMapper.updateById(f);
        return lockStatusInternal(f);
    }

    @Override
    @Transactional
    public void releaseLock(Long fileId, Long userId) {
        com.rocgit.knife4j.entity.file.FileEntity f = fileMapper.selectById(fileId);
        if (f == null) return;
        Long cur = f.getLockUserId() == null ? 0L : f.getLockUserId();
        // 仅持有者可释放；非持有者或已过期忽略
        if (cur > 0 && cur.equals(userId)) {
            f.setLockUserId(0L);
            f.setLockExpireAt(null);
            fileMapper.updateById(f);
        }
    }

    @Override
    public Map<String, Object> lockStatus(Long fileId) {
        com.rocgit.knife4j.entity.file.FileEntity f = fileMapper.selectById(fileId);
        if (f == null) throw new BusinessException(ResultCode.FILE_NOT_FOUND);
        return lockStatusInternal(f);
    }

    private Map<String, Object> lockStatusInternal(com.rocgit.knife4j.entity.file.FileEntity f) {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        Long cur = f.getLockUserId() == null ? 0L : f.getLockUserId();
        java.time.LocalDateTime expire = f.getLockExpireAt();
        boolean active = cur > 0 && expire != null && expire.isAfter(now);
        long seconds = active ? java.time.Duration.between(now, expire).getSeconds() : 0;
        Map<String, Object> data = new java.util.LinkedHashMap<>();
        data.put("fileId", f.getId());
        data.put("locked", active);
        data.put("lockUserId", active ? cur : 0);
        data.put("expireAt", active ? expire : null);
        data.put("secondsLeft", seconds);
        return data;
    }

    private FileVO toFileVO(com.rocgit.knife4j.entity.file.FileEntity f) {
        FileVO vo = new FileVO();
        vo.setFileId(f.getId());
        vo.setProjectId(f.getProjectId());
        vo.setStageId(f.getStageId());
        vo.setUploaderId(f.getUploaderId());
        vo.setName(f.getName());
        vo.setUrl(f.getUrl());
        vo.setSize(f.getSize());
        vo.setType(f.getType());
        vo.setStatus(f.getStatus());
        vo.setVersion(f.getVersion());
        vo.setCreatedAt(f.getCreatedAt());
        return vo;
    }
}