package com.rocgit.knife4j.service;

import com.rocgit.knife4j.entity.project.ContributionStatsVO;
import com.rocgit.knife4j.entity.file.FileAuditLog;
import com.rocgit.knife4j.entity.file.FileEntity;
import com.rocgit.knife4j.entity.group.GroupMember;
import com.rocgit.knife4j.entity.task.Task;
import com.rocgit.knife4j.entity.User;
import com.rocgit.knife4j.entity.Project;
import com.rocgit.knife4j.mapper.ProjectMapper;
import com.rocgit.knife4j.mapper.UserMapper;
import com.rocgit.knife4j.mapper.file.FileAuditLogMapper;
import com.rocgit.knife4j.mapper.file.FileMapper;
import com.rocgit.knife4j.mapper.group.GroupMemberMapper;
import com.rocgit.knife4j.mapper.task.TaskMapper;
import com.rocgit.knife4j.service.project.impl.ProjectServiceImpl;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * T-Q-2：ProjectService.contributionStats 单元测试
 * 验证：积分公式（5/3/1/8/-2）正确；0 分保底；按 score 降序赋 rank
 */
class ContributionStatsServiceTest {

    @Test
    void scoreFormula_shouldBeUpload5Audit3Download1Completed8OverdueMinus2() {
        // mock 依赖
        ProjectMapper projectMapper = mock(ProjectMapper.class);
        UserMapper userMapper = mock(UserMapper.class);
        GroupMemberMapper groupMemberMapper = mock(GroupMemberMapper.class);
        FileMapper fileMapper = mock(FileMapper.class);
        FileAuditLogMapper fileAuditLogMapper = mock(FileAuditLogMapper.class);
        TaskMapper taskMapper = mock(TaskMapper.class);
        com.rocgit.knife4j.mapper.stage.StageMapper stageMapper = mock(com.rocgit.knife4j.mapper.stage.StageMapper.class);
        com.rocgit.knife4j.mapper.stage.StageTemplateMapper stageTemplateMapper = mock(com.rocgit.knife4j.mapper.stage.StageTemplateMapper.class);
        com.rocgit.knife4j.mapper.log.OperationLogMapper operationLogMapper = mock(com.rocgit.knife4j.mapper.log.OperationLogMapper.class);

        Project p = new Project();
        p.setId(1L);
        p.setGroupId(10L);
        p.setName("Test");
        p.setStartDate(LocalDate.now());
        when(projectMapper.selectById(1L)).thenReturn(p);

        // 2 个成员
        GroupMember gm1 = new GroupMember();
        gm1.setUserId(100L);
        GroupMember gm2 = new GroupMember();
        gm2.setUserId(200L);
        when(groupMemberMapper.selectList(any())).thenReturn(Arrays.asList(gm1, gm2));

        User u1 = new User();
        u1.setId(100L);
        u1.setNickname("Alice");
        User u2 = new User();
        u2.setId(200L);
        u2.setNickname("Bob");
        when(userMapper.selectById(100L)).thenReturn(u1);
        when(userMapper.selectById(200L)).thenReturn(u2);

        // Alice: 2 upload + 1 audit + 0 download + 1 completed + 0 overdue = 2*5 + 1*3 + 0 + 1*8 + 0 = 21
        // Bob:   0 upload + 0 audit + 5 download + 0 completed + 3 overdue = 0 + 0 + 5*1 + 0 + 3*-2 = -1 → 0
        when(fileMapper.selectCount(any())).thenReturn(0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L);
        // Alice: upload=2, Bob:upload=0
        when(fileMapper.selectCount(any())).thenReturn(
                2L, // Alice upload
                0L, // Bob upload
                0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L
        );
        // audit count: Alice=1, Bob=0
        when(fileAuditLogMapper.selectCount(any())).thenReturn(
                1L, // Alice audit
                0L, // Bob audit
                0L, // Alice download
                5L, // Bob download
                1L, // Alice completed
                0L, // Bob completed
                0L, // Alice overdue
                3L  // Bob overdue
        );
        // task count
        when(taskMapper.selectCount(any())).thenReturn(
                1L, // Alice completed
                0L, // Bob completed
                0L, // Alice overdue
                3L  // Bob overdue
        );
        // last active
        when(taskMapper.selectOne(any())).thenReturn(null);
        when(fileMapper.selectOne(any())).thenReturn(null);
        when(fileAuditLogMapper.selectOne(any())).thenReturn(null);

        ProjectServiceImpl service = new ProjectServiceImpl();
        org.springframework.test.util.ReflectionTestUtils.setField(service, "projectMapper", projectMapper);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "stageMapper", stageMapper);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "stageTemplateMapper", stageTemplateMapper);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "taskMapper", taskMapper);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "fileMapper", fileMapper);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "fileAuditLogMapper", fileAuditLogMapper);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "userMapper", userMapper);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "operationLogMapper", operationLogMapper);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "groupMemberMapper", groupMemberMapper);

        List<ContributionStatsVO> result = service.contributionStats(1L);
        assertEquals(2, result.size());
        // Alice first (score 21), Bob second (score 0)
        assertEquals(100L, result.get(0).getUserId());
        assertEquals("Alice", result.get(0).getNickname());
        assertEquals(21, result.get(0).getScore());
        assertEquals(1, result.get(0).getRank());

        assertEquals(200L, result.get(1).getUserId());
        assertEquals("Bob", result.get(1).getNickname());
        // 0 + 0 + 5*1 + 0 + 3*-2 = -1 → Math.max(0, -1) = 0
        assertEquals(0, result.get(1).getScore());
        assertEquals(2, result.get(1).getRank());
    }

    @Test
    void contributionStats_emptyProject_shouldReturnEmptyList() {
        ProjectMapper projectMapper = mock(ProjectMapper.class);
        when(projectMapper.selectById(99L)).thenReturn(null);

        ProjectServiceImpl service = new ProjectServiceImpl();
        org.springframework.test.util.ReflectionTestUtils.setField(service, "projectMapper", projectMapper);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "groupMemberMapper", mock(GroupMemberMapper.class));

        List<ContributionStatsVO> result = service.contributionStats(99L);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
