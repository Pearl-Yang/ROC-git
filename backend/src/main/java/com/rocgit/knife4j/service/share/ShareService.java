package com.rocgit.knife4j.service.share;

import com.rocgit.knife4j.dto.share.*;
import com.rocgit.knife4j.entity.share.AccessLogVO;
import com.rocgit.knife4j.entity.share.ShareStatsVO;
import com.rocgit.knife4j.entity.share.ShareVO;

import java.util.List;
import java.util.Map;

public interface ShareService {

    ShareVO create(Long userId, ShareCreateRequest request);

    void update(Long shareId, ShareUpdateRequest request);

    Map<String, Object> validate(ShareValidateRequest request);

    void delete(Long shareId);

    List<ShareVO> myShares(Long userId);

    ShareStatsVO stats(Long shareId);

    void recordVisit(Long shareId, String ip, String userAgent);

    List<AccessLogVO> accessLogs(Long shareId);

    /**
     * T-02b：访客视图装配。按 permissionLevel 裁剪：
     * view_only          → 只返 project + stage
     * view_with_preview  → + file 列表（name/type/size，不返 url）
     * view_with_download → + file 列表（带 url/downloadUrl）
     */
    Map<String, Object> buildVisitorView(String shareToken, String password);

    /**
     * T-02c：访客通过 token 进入时记一条访问流水（公开接口内部调用）。
     */
    void recordVisitByToken(String shareToken, String ip, String userAgent);
}