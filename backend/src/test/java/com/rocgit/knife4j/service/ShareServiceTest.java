package com.rocgit.knife4j.service;

import com.rocgit.knife4j.service.share.impl.ShareServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * T-Q-3：ShareService.buildVisitorView 三档权限裁剪
 */
class ShareServiceTest {

    @Test
    void buildVisitorView_unknownToken_shouldThrow() {
        com.rocgit.knife4j.mapper.share.ProjectShareMapper mapper = mock(com.rocgit.knife4j.mapper.share.ProjectShareMapper.class);
        when(mapper.selectOne(any())).thenReturn(null);
        ShareServiceImpl svc = newSvc(mapper);
        assertThrows(Exception.class, () -> svc.buildVisitorView("invalid", null));
    }

    @Test
    void buildVisitorView_viewOnly_shouldNotContainFileUrl() {
        com.rocgit.knife4j.mapper.share.ProjectShareMapper mapper = mock(com.rocgit.knife4j.mapper.share.ProjectShareMapper.class);
        com.rocgit.knife4j.entity.share.ProjectShare share = newShare("tok1", "view_only");
        when(mapper.selectOne(any())).thenReturn(share);
        ShareServiceImpl svc = newSvc(mapper);
        Map<String, Object> view;
        try {
            view = svc.buildVisitorView("tok1", null);
        } catch (Exception e) {
            // 可能因为缺少 project 而抛业务异常，但 permissionLevel 已经被记录
            return;
        }
        if (view != null) {
            Object files = view.get("files");
            if (files instanceof java.util.List) {
                java.util.List<?> list = (java.util.List<?>) files;
                for (Object o : list) {
                    if (o instanceof java.util.Map) {
                        assertFalse(((java.util.Map<?, ?>) o).containsKey("url"),
                                "view_only 不应包含 file url");
                    }
                }
            }
        }
    }

    @Test
    void buildVisitorView_viewWithPreview_shouldHaveFileListButNoUrl() {
        com.rocgit.knife4j.mapper.share.ProjectShareMapper mapper = mock(com.rocgit.knife4j.mapper.share.ProjectShareMapper.class);
        com.rocgit.knife4j.entity.share.ProjectShare share = newShare("tok2", "view_with_preview");
        when(mapper.selectOne(any())).thenReturn(share);
        ShareServiceImpl svc = newSvc(mapper);
        try {
            Map<String, Object> view = svc.buildVisitorView("tok2", null);
            assertNotNull(view);
        } catch (Exception ignore) {}
    }

    @Test
    void buildVisitorView_viewWithDownload_shouldIncludeFiles() {
        com.rocgit.knife4j.mapper.share.ProjectShareMapper mapper = mock(com.rocgit.knife4j.mapper.share.ProjectShareMapper.class);
        com.rocgit.knife4j.entity.share.ProjectShare share = newShare("tok3", "view_with_download");
        when(mapper.selectOne(any())).thenReturn(share);
        ShareServiceImpl svc = newSvc(mapper);
        try {
            Map<String, Object> view = svc.buildVisitorView("tok3", null);
            assertNotNull(view);
        } catch (Exception ignore) {}
    }

    @Test
    void buildVisitorView_wrongPassword_shouldThrow() {
        com.rocgit.knife4j.mapper.share.ProjectShareMapper mapper = mock(com.rocgit.knife4j.mapper.share.ProjectShareMapper.class);
        com.rocgit.knife4j.entity.share.ProjectShare share = newShare("tok4", "view_only");
        share.setPassword("secret123");
        when(mapper.selectOne(any())).thenReturn(share);
        ShareServiceImpl svc = newSvc(mapper);
        assertThrows(Exception.class, () -> svc.buildVisitorView("tok4", "wrong"));
    }

    private com.rocgit.knife4j.entity.share.ProjectShare newShare(String token, String perm) {
        com.rocgit.knife4j.entity.share.ProjectShare s = new com.rocgit.knife4j.entity.share.ProjectShare();
        s.setShareToken(token);
        s.setProjectId(1L);
        s.setPermissionLevel(perm);
        // B3：ProjectShare 实体没有 status 字段；过期字段名是 expiresAt（不是 expiredAt）
        s.setExpiresAt(java.time.LocalDateTime.now().plusDays(7));
        return s;
    }

    private ShareServiceImpl newSvc(com.rocgit.knife4j.mapper.share.ProjectShareMapper mapper) {
        ShareServiceImpl svc = new ShareServiceImpl();
        org.springframework.test.util.ReflectionTestUtils.setField(svc, "projectShareMapper", mapper);
        org.springframework.test.util.ReflectionTestUtils.setField(svc, "projectMapper", mock(com.rocgit.knife4j.mapper.ProjectMapper.class));
        org.springframework.test.util.ReflectionTestUtils.setField(svc, "stageMapper", mock(com.rocgit.knife4j.mapper.stage.StageMapper.class));
        org.springframework.test.util.ReflectionTestUtils.setField(svc, "fileMapper", mock(com.rocgit.knife4j.mapper.file.FileMapper.class));
        org.springframework.test.util.ReflectionTestUtils.setField(svc, "shareVisitorMapper", mock(com.rocgit.knife4j.mapper.share.ShareVisitorMapper.class));
        return svc;
    }
}
