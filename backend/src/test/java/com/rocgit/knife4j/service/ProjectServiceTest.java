package com.rocgit.knife4j.service;

import com.rocgit.knife4j.common.BusinessException;
import com.rocgit.knife4j.entity.Project;
import com.rocgit.knife4j.mapper.ProjectMapper;
import com.rocgit.knife4j.service.project.impl.ProjectServiceImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * T-Q-3：ProjectService create / copy / complete
 */
class ProjectServiceTest {

    private static ProjectMapper mockProjectMapper() {
        return mock(ProjectMapper.class);
    }

    private static ProjectServiceImpl newSvc() {
        ProjectServiceImpl svc = new ProjectServiceImpl();
        org.springframework.test.util.ReflectionTestUtils.setField(svc, "projectMapper", mockProjectMapper());
        org.springframework.test.util.ReflectionTestUtils.setField(svc, "groupMemberMapper", mock(com.rocgit.knife4j.mapper.group.GroupMemberMapper.class));
        org.springframework.test.util.ReflectionTestUtils.setField(svc, "stageMapper", mock(com.rocgit.knife4j.mapper.stage.StageMapper.class));
        org.springframework.test.util.ReflectionTestUtils.setField(svc, "stageTemplateMapper", mock(com.rocgit.knife4j.mapper.stage.StageTemplateMapper.class));
        org.springframework.test.util.ReflectionTestUtils.setField(svc, "taskMapper", mock(com.rocgit.knife4j.mapper.task.TaskMapper.class));
        org.springframework.test.util.ReflectionTestUtils.setField(svc, "fileMapper", mock(com.rocgit.knife4j.mapper.file.FileMapper.class));
        org.springframework.test.util.ReflectionTestUtils.setField(svc, "fileAuditLogMapper", mock(com.rocgit.knife4j.mapper.file.FileAuditLogMapper.class));
        org.springframework.test.util.ReflectionTestUtils.setField(svc, "userMapper", mock(com.rocgit.knife4j.mapper.UserMapper.class));
        org.springframework.test.util.ReflectionTestUtils.setField(svc, "operationLogMapper", mock(com.rocgit.knife4j.mapper.log.OperationLogMapper.class));
        return svc;
    }

    @Test
    void detail_nullId_shouldThrow() {
        ProjectServiceImpl svc = newSvc();
        ProjectMapper pm = (ProjectMapper) org.springframework.test.util.ReflectionTestUtils.getField(svc, "projectMapper");
        when(pm.selectById(999L)).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class, () -> svc.detail(999L));
        assertNotNull(ex);
    }

    @Test
    void complete_alreadyCompleted_shouldStillSucceed() {
        ProjectServiceImpl svc = newSvc();
        ProjectMapper pm = (ProjectMapper) org.springframework.test.util.ReflectionTestUtils.getField(svc, "projectMapper");
        Project p = new Project();
        p.setId(1L);
        p.setName("p");
        p.setStatus("completed");
        when(pm.selectById(1L)).thenReturn(p);
        when(pm.updateById(any())).thenReturn(1);
        assertDoesNotThrow(() -> svc.complete(1L));
    }

    @Test
    void complete_nullProject_shouldThrow() {
        ProjectServiceImpl svc = newSvc();
        ProjectMapper pm = (ProjectMapper) org.springframework.test.util.ReflectionTestUtils.getField(svc, "projectMapper");
        when(pm.selectById(99L)).thenReturn(null);
        assertThrows(Exception.class, () -> svc.complete(99L));
    }

    @Test
    void copy_nullSource_shouldThrow() {
        ProjectServiceImpl svc = newSvc();
        ProjectMapper pm = (ProjectMapper) org.springframework.test.util.ReflectionTestUtils.getField(svc, "projectMapper");
        when(pm.selectById(99L)).thenReturn(null);
        // B3：当前签名 copy(Long userId, Long projectId, CopyProjectRequest request)
        com.rocgit.knife4j.dto.project.CopyProjectRequest req = new com.rocgit.knife4j.dto.project.CopyProjectRequest();
        req.setName("副本");
        req.setIncludeFiles(true);
        BusinessException ex = assertThrows(BusinessException.class, () -> svc.copy(99L, 1L, req));
        assertNotNull(ex);
    }

    @Test
    void delete_alreadyDeleted_shouldThrow() {
        ProjectServiceImpl svc = newSvc();
        ProjectMapper pm = (ProjectMapper) org.springframework.test.util.ReflectionTestUtils.getField(svc, "projectMapper");
        Project p = new Project();
        p.setId(1L);
        p.setDeleted(1);
        when(pm.selectById(1L)).thenReturn(p);
        BusinessException ex = assertThrows(BusinessException.class, () -> svc.delete(1L));
        assertNotNull(ex);
    }

    @Test
    void delete_normal_shouldWork() {
        ProjectServiceImpl svc = newSvc();
        ProjectMapper pm = (ProjectMapper) org.springframework.test.util.ReflectionTestUtils.getField(svc, "projectMapper");
        Project p = new Project();
        p.setId(1L);
        p.setDeleted(0);
        when(pm.selectById(1L)).thenReturn(p);
        when(pm.updateById(any())).thenReturn(1);
        assertDoesNotThrow(() -> svc.delete(1L));
    }
}
