package com.rocgit.knife4j.service.share.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.rocgit.knife4j.common.BusinessException;
import com.rocgit.knife4j.common.ResultCode;
import com.rocgit.knife4j.dto.share.*;
import com.rocgit.knife4j.entity.Project;
import com.rocgit.knife4j.entity.share.ProjectShare;
import com.rocgit.knife4j.entity.share.ShareVisitor;
import com.rocgit.knife4j.entity.share.AccessLogVO;
import com.rocgit.knife4j.entity.share.ShareStatsVO;
import com.rocgit.knife4j.entity.share.ShareVO;
import com.rocgit.knife4j.mapper.ProjectMapper;
import com.rocgit.knife4j.mapper.file.FileMapper;
import com.rocgit.knife4j.mapper.share.ProjectShareMapper;
import com.rocgit.knife4j.mapper.share.ShareVisitorMapper;
import com.rocgit.knife4j.mapper.stage.StageMapper;
import com.rocgit.knife4j.entity.stage.Stage;
import com.rocgit.knife4j.service.share.ShareService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class ShareServiceImpl implements ShareService {

    @Autowired private ProjectShareMapper projectShareMapper;
    @Autowired private ShareVisitorMapper shareVisitorMapper;
    @Autowired private ProjectMapper projectMapper;
    @Autowired private StageMapper stageMapper;
    @Autowired private FileMapper fileMapper;

    /** 分享链接 base URL（可由 application.yml 配置 rocgit.share.base-url 覆盖） */
    @org.springframework.beans.factory.annotation.Value("${rocgit.share.base-url:http://127.0.0.1:8080/view}")
    private String shareBaseUrl;

    @Override
    @Transactional
    public ShareVO create(Long userId, ShareCreateRequest request) {
        ProjectShare s = new ProjectShare();
        s.setProjectId(request.getProjectId());
        s.setShareToken("share_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        s.setPermissionLevel(request.getPermissionLevel() == null ? "view_only" : request.getPermissionLevel());
        s.setViewCount(0);
        s.setCreatorId(userId == null ? 0L : userId);
        s.setExpiresAt(request.getExpiresAt() == null ? LocalDateTime.now().plusMonths(6) : request.getExpiresAt());
        s.setPassword(Boolean.TRUE.equals(request.getNeedPassword()) ? request.getPassword() : "");
        projectShareMapper.insert(s);
        return toVO(s);
    }

    @Override
    public void update(Long shareId, ShareUpdateRequest request) {
        ProjectShare s = projectShareMapper.selectById(shareId);
        if (s == null) throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "分享不存在");
        if (request.getPermissionLevel() != null) s.setPermissionLevel(request.getPermissionLevel());
        if (request.getExpiresAt() != null) s.setExpiresAt(request.getExpiresAt());
        // T-02 完善：支持改密码 / 取消密码
        if (request.getNeedPassword() != null) {
            if (Boolean.TRUE.equals(request.getNeedPassword())) {
                String pwd = request.getPassword() == null ? "" : request.getPassword();
                if (pwd.length() < 4 || pwd.length() > 32) {
                    throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "密码长度需 4-32 位");
                }
                s.setPassword(pwd);
            } else {
                s.setPassword("");
            }
        }
        projectShareMapper.updateById(s);
    }

    @Override
    public Map<String, Object> validate(ShareValidateRequest request) {
        Map<String, Object> result = new HashMap<>();
        if (request.getShareToken() == null) {
            result.put("valid", false);
            return result;
        }
        ProjectShare s = projectShareMapper.selectOne(
                new QueryWrapper<ProjectShare>().eq("share_token", request.getShareToken()).eq("deleted", 0).last("LIMIT 1"));
        if (s == null) {
            result.put("valid", false);
            return result;
        }
        // T-02d 完整版访客视图：校验密码
        if (s.getPassword() != null && !s.getPassword().isEmpty()
                && !Objects.equals(s.getPassword(), request.getPassword() == null ? "" : request.getPassword())) {
            result.put("valid", false);
            result.put("error", "password_required");
            return result;
        }
        // 校验过期
        if (s.getExpiresAt() != null && s.getExpiresAt().isBefore(LocalDateTime.now())) {
            result.put("valid", false);
            result.put("error", "expired");
            return result;
        }
        result.put("valid", true);
        result.put("projectId", s.getProjectId());
        result.put("permissionLevel", s.getPermissionLevel());
        result.put("viewCount", s.getViewCount());
        result.put("expiresAt", s.getExpiresAt());
        result.put("needPassword", s.getPassword() != null && !s.getPassword().isEmpty());
        return result;
    }

    @Override
    @Transactional
    public void delete(Long shareId) {
        ProjectShare s = projectShareMapper.selectById(shareId);
        if (s == null) return;
        s.setDeleted(1);
        projectShareMapper.updateById(s);
    }

    @Override
    public List<ShareVO> myShares(Long userId) {
        // T-02a：按 creator_id 过滤
        List<ProjectShare> list = projectShareMapper.selectList(
                new QueryWrapper<ProjectShare>()
                        .eq(userId != null && userId > 0, "creator_id", userId == null ? 0L : userId)
                        .eq("deleted", 0).orderByDesc("created_at").last("LIMIT 50"));
        List<ShareVO> result = new ArrayList<>();
        for (ProjectShare s : list) result.add(toVO(s));
        return result;
    }

    @Override
    public ShareStatsVO stats(Long shareId) {
        ProjectShare s = projectShareMapper.selectById(shareId);
        ShareStatsVO vo = new ShareStatsVO();
        if (s != null) {
            Long v = shareVisitorMapper.selectCount(
                    new QueryWrapper<ShareVisitor>().eq("share_id", shareId).eq("deleted", 0));
            vo.setViewCount(s.getViewCount() == null ? 0 : s.getViewCount());
            vo.setVisitorCount(v == null ? 0 : v.intValue());
        } else {
            vo.setViewCount(0);
            vo.setVisitorCount(0);
        }
        return vo;
    }

    @Override
    @Transactional
    public void recordVisit(Long shareId, String ip, String userAgent) {
        ProjectShare s = projectShareMapper.selectById(shareId);
        if (s != null) {
            s.setViewCount((s.getViewCount() == null ? 0 : s.getViewCount()) + 1);
            projectShareMapper.updateById(s);
        }
        ShareVisitor v = new ShareVisitor();
        v.setShareId(shareId);
        v.setIp(ip == null ? "" : ip);
        v.setUserAgent(userAgent == null ? "" : userAgent);
        v.setVisitedAt(LocalDateTime.now());
        shareVisitorMapper.insert(v);
    }

    /**
     * T-02：访客通过 shareToken 进入时记录访问（公开接口内部调用）。
     */
    @Override
    @Transactional
    public void recordVisitByToken(String shareToken, String ip, String userAgent) {
        if (shareToken == null || shareToken.isEmpty()) return;
        ProjectShare s = projectShareMapper.selectOne(
                new QueryWrapper<ProjectShare>().eq("share_token", shareToken).eq("deleted", 0).last("LIMIT 1"));
        if (s == null) return;
        s.setViewCount((s.getViewCount() == null ? 0 : s.getViewCount()) + 1);
        projectShareMapper.updateById(s);
        ShareVisitor v = new ShareVisitor();
        v.setShareId(s.getId());
        v.setIp(ip == null ? "" : ip);
        v.setUserAgent(userAgent == null ? "" : userAgent);
        v.setVisitedAt(LocalDateTime.now());
        shareVisitorMapper.insert(v);
    }

    @Override
    public List<AccessLogVO> accessLogs(Long shareId) {
        List<ShareVisitor> list = shareVisitorMapper.selectList(
                new QueryWrapper<ShareVisitor>().eq("share_id", shareId).eq("deleted", 0).orderByDesc("visited_at").last("LIMIT 100"));
        List<AccessLogVO> result = new ArrayList<>();
        for (ShareVisitor v : list) {
            AccessLogVO vo = new AccessLogVO();
            vo.setId(v.getId());
            vo.setShareId(v.getShareId());
            vo.setIp(v.getIp());
            vo.setUserAgent(v.getUserAgent());
            vo.setVisitedAt(v.getVisitedAt());
            result.add(vo);
        }
        return result;
    }

    /**
     * T-02b 新增：访客视图数据装配，按 permissionLevel 裁剪
     * view_only          → 只返 project + stage 列表
     * view_with_preview  → + file 列表（仅 name/type/size，不返 url/downloadUrl）
     * view_with_download → + file 列表（带 url/downloadUrl）
     */
    public Map<String, Object> buildVisitorView(String shareToken, String password) {
        ProjectShare s = projectShareMapper.selectOne(
                new QueryWrapper<ProjectShare>().eq("share_token", shareToken).eq("deleted", 0).last("LIMIT 1"));
        if (s == null) throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "分享不存在");
        if (s.getExpiresAt() != null && s.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "分享已过期");
        }
        if (s.getPassword() != null && !s.getPassword().isEmpty() && !Objects.equals(s.getPassword(), password == null ? "" : password)) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "密码错误");
        }

        Project p = projectMapper.selectById(s.getProjectId());
        Map<String, Object> data = new HashMap<>();
        if (p == null) {
            data.put("project", null);
            return data;
        }
        Map<String, Object> proj = new LinkedHashMap<>();
        proj.put("id", p.getId());
        proj.put("name", p.getName());
        proj.put("description", p.getDescription());
        proj.put("currentStage", p.getCurrentStage());
        proj.put("progress", p.getProgress());
        proj.put("status", p.getStatus());
        data.put("project", proj);

        // stages 全部返
        List<Stage> stages = stageMapper.selectList(
                new QueryWrapper<Stage>().eq("project_id", p.getId()).eq("deleted", 0).orderByAsc("order_num"));
        data.put("stages", stages);

        // 文件按权限裁剪
        String level = s.getPermissionLevel() == null ? "view_only" : s.getPermissionLevel();
        if ("view_with_preview".equals(level) || "view_with_download".equals(level)) {
            List<com.rocgit.knife4j.entity.file.FileEntity> files = fileMapper.selectList(
                    new QueryWrapper<com.rocgit.knife4j.entity.file.FileEntity>()
                            .eq("project_id", p.getId()).eq("deleted", 0).orderByDesc("created_at"));
            List<Map<String, Object>> fileList = new ArrayList<>();
            for (com.rocgit.knife4j.entity.file.FileEntity f : files) {
                Map<String, Object> fi = new LinkedHashMap<>();
                fi.put("fileId", f.getId());
                fi.put("name", f.getName());
                fi.put("type", f.getType());
                fi.put("size", f.getSize());
                fi.put("stageId", f.getStageId());
                fi.put("version", f.getVersion());
                fi.put("createdAt", f.getCreatedAt());
                if ("view_with_download".equals(level)) {
                    fi.put("url", f.getUrl());
                    fi.put("downloadUrl", f.getUrl());
                }
                fileList.add(fi);
            }
            data.put("files", fileList);
        }
        data.put("permissionLevel", level);
        data.put("shareToken", shareToken);
        return data;
    }

    private ShareVO toVO(ProjectShare s) {
        ShareVO vo = new ShareVO();
        vo.setShareId(s.getId());
        vo.setProjectId(s.getProjectId());
        vo.setShareToken(s.getShareToken());
        String base = shareBaseUrl.endsWith("/") ? shareBaseUrl.substring(0, shareBaseUrl.length() - 1) : shareBaseUrl;
        vo.setShareUrl(base + "?token=" + s.getShareToken());
        vo.setQrCodeUrl(""); // 二维码前端用 wxqrcode.js 自渲染（baseUrl + token）
        vo.setPermissionLevel(s.getPermissionLevel());
        vo.setViewCount(s.getViewCount());
        vo.setExpiresAt(s.getExpiresAt());
        vo.setCreatedAt(s.getCreatedAt());
        vo.setNeedPassword(s.getPassword() != null && !s.getPassword().isEmpty());
        return vo;
    }
}