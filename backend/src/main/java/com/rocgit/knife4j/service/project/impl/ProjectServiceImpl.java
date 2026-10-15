package com.rocgit.knife4j.service.project.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.rocgit.knife4j.common.BusinessException;
import com.rocgit.knife4j.common.ResultCode;
import com.rocgit.knife4j.dto.project.*;
import com.rocgit.knife4j.entity.Project;
import com.rocgit.knife4j.entity.log.OperationLog;
import com.rocgit.knife4j.entity.stage.Stage;
import com.rocgit.knife4j.entity.task.Task;
import com.rocgit.knife4j.entity.file.FileEntity;
import com.rocgit.knife4j.entity.file.FileAuditLog;
import com.rocgit.knife4j.entity.project.*;
import com.rocgit.knife4j.entity.stage.StageTemplate;
import com.rocgit.knife4j.entity.User;
import com.rocgit.knife4j.mapper.ProjectMapper;
import com.rocgit.knife4j.mapper.group.GroupMemberMapper;
import com.rocgit.knife4j.mapper.stage.StageMapper;
import com.rocgit.knife4j.mapper.stage.StageTemplateMapper;
import com.rocgit.knife4j.mapper.task.TaskMapper;
import com.rocgit.knife4j.mapper.file.FileMapper;
import com.rocgit.knife4j.mapper.file.FileAuditLogMapper;
import com.rocgit.knife4j.mapper.log.OperationLogMapper;
import com.rocgit.knife4j.mapper.UserMapper;
import com.rocgit.knife4j.service.project.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class ProjectServiceImpl implements ProjectService {

    @Autowired private ProjectMapper projectMapper;
    @Autowired private StageMapper stageMapper;
    @Autowired private StageTemplateMapper stageTemplateMapper;
    @Autowired private TaskMapper taskMapper;
    @Autowired private FileMapper fileMapper;
    @Autowired private FileAuditLogMapper fileAuditLogMapper; // T-06
    @Autowired private UserMapper userMapper;
    @Autowired private OperationLogMapper operationLogMapper;
    @Autowired private GroupMemberMapper groupMemberMapper;

    @Override
    @Transactional
    public ProjectDetailVO create(Long userId, CreateProjectRequest request) {
        Project p = new Project();
        p.setGroupId(request.getGroupId());
        p.setName(request.getName() == null ? "未命名项目" : request.getName());
        p.setDescription(request.getDescription());
        p.setStatus("active");
        p.setProgress(0);
        p.setStartDate(LocalDate.now());
        p.setEndDate(LocalDate.now().plusMonths(3));
        projectMapper.insert(p);

        // 记录日志
        OperationLog log = new OperationLog();
        log.setProjectId(p.getId());
        log.setUserId(userId);
        log.setAction("create_project");
        log.setTargetType("project");
        log.setTargetId(p.getId());
        log.setDescription("创建项目：" + p.getName());
        operationLogMapper.insert(log);

        return detail(p.getId());
    }

    @Override
    public List<ProjectVO> list(Long groupId) {
        List<Project> list = projectMapper.selectList(
                new QueryWrapper<Project>().eq("group_id", groupId).eq("deleted", 0));
        List<ProjectVO> result = new ArrayList<>();
        for (Project p : list) {
            ProjectVO vo = new ProjectVO();
            vo.setProjectId(p.getId());
            vo.setGroupId(p.getGroupId());
            vo.setName(p.getName());
            vo.setDescription(p.getDescription());
            vo.setStatus(p.getStatus());
            vo.setProgress(p.getProgress());
            vo.setCreatedAt(p.getCreatedAt());
            result.add(vo);
        }
        return result;
    }

    @Override
    public ProjectDetailVO detail(Long projectId) {
        Project p = projectMapper.selectById(projectId);
        if (p == null || (p.getDeleted() != null && p.getDeleted() == 1)) {
            throw new BusinessException(ResultCode.PROJECT_NOT_FOUND.getCode());
        }
        ProjectDetailVO vo = new ProjectDetailVO();
        vo.setProjectId(p.getId());
        vo.setGroupId(p.getGroupId());
        vo.setName(p.getName());
        vo.setDescription(p.getDescription());
        vo.setCurrentStage(p.getCurrentStage() == null ? "" : p.getCurrentStage());
        vo.setProgress(p.getProgress());
        vo.setStatus(p.getStatus());
        vo.setStages(stageList(projectId));
        Long mc = groupMemberMapper.selectCount(
                new QueryWrapper<com.rocgit.knife4j.entity.group.GroupMember>().eq("group_id", p.getGroupId()).eq("deleted", 0));
        vo.setMemberCount(mc == null ? 0 : mc.intValue());
        vo.setFileCount((int) (long) fileMapper.selectCount(
                new QueryWrapper<FileEntity>().eq("project_id", projectId).eq("deleted", 0).eq("is_latest", 1)));
        vo.setTaskCount((int) (long) taskMapper.selectCount(
                new QueryWrapper<Task>().eq("project_id", projectId).eq("deleted", 0)));
        vo.setCompletedTaskCount((int) (long) taskMapper.selectCount(
                new QueryWrapper<Task>().eq("project_id", projectId).eq("deleted", 0).eq("status", "completed")));
        vo.setCreatedAt(p.getCreatedAt());
        return vo;
    }

    @Override
    public void update(Long projectId, UpdateProjectRequest request) {
        Project p = projectMapper.selectById(projectId);
        if (p == null) throw new BusinessException(ResultCode.PROJECT_NOT_FOUND.getCode());
        if (request.getName() != null) p.setName(request.getName());
        if (request.getDescription() != null) p.setDescription(request.getDescription());
        projectMapper.updateById(p);
    }

    @Override
    @Transactional
    public void delete(Long projectId) {
        Project p = projectMapper.selectById(projectId);
        if (p == null) throw new BusinessException(ResultCode.PROJECT_NOT_FOUND.getCode());
        p.setDeleted(1);
        projectMapper.updateById(p);
    }

    @Override
    @Transactional
    public ProjectVO copy(Long userId, Long projectId, CopyProjectRequest request) {
        Project src = projectMapper.selectById(projectId);
        if (src == null) throw new BusinessException(ResultCode.PROJECT_NOT_FOUND.getCode());
        Project copy = new Project();
        copy.setGroupId(src.getGroupId());
        copy.setName(request.getName() == null ? (src.getName() + " (副本)") : request.getName());
        copy.setDescription(src.getDescription());
        copy.setStatus("active");
        copy.setProgress(0);
        copy.setStartDate(LocalDate.now());
        copy.setEndDate(LocalDate.now().plusMonths(3));
        projectMapper.insert(copy);

        // T-05：复制阶段（按 order_num 升序），记录旧ID→新ID 映射
        boolean includeTasks = request.getIncludeTasks() != null && request.getIncludeTasks();
        boolean includeFiles = request.getIncludeFiles() != null && request.getIncludeFiles();
        List<Stage> srcStages = stageMapper.selectList(
                new QueryWrapper<Stage>().eq("project_id", projectId).eq("deleted", 0).orderByAsc("order_num"));
        Map<Long, Long> oldStageId2New = new HashMap<>();
        for (Stage s : srcStages) {
            Stage n = new Stage();
            n.setProjectId(copy.getId());
            n.setName(s.getName());
            n.setOrderNum(s.getOrderNum());
            n.setStartDate(s.getStartDate());
            n.setEndDate(s.getEndDate());
            n.setStatus("pending");
            stageMapper.insert(n);
            oldStageId2New.put(s.getId(), n.getId());
        }

        // T-05：复制任务（阶段重新归属；负责人沿用；状态重置为 pending；截止时间顺延至今天起算）
        if (includeTasks) {
            List<Task> srcTasks = taskMapper.selectList(
                    new QueryWrapper<Task>().eq("project_id", projectId).eq("deleted", 0));
            for (Task t : srcTasks) {
                Task n = new Task();
                n.setProjectId(copy.getId());
                Long mappedStage = oldStageId2New.get(t.getStageId());
                n.setStageId(mappedStage == null ? 0L : mappedStage);
                n.setTitle(t.getTitle());
                n.setDescription(t.getDescription());
                n.setAssigneeId(t.getAssigneeId());
                n.setStatus("pending");
                // 把截止时间平移到新项目的今天起点
                if (t.getDeadline() != null && src.getStartDate() != null) {
                    long offsetDays = java.time.temporal.ChronoUnit.DAYS.between(src.getStartDate(), LocalDate.now());
                    n.setDeadline(t.getDeadline().plusDays(offsetDays));
                } else {
                    n.setDeadline(t.getDeadline());
                }
                taskMapper.insert(n);
            }
        }

        // T-05：复制文件行（不复制磁盘文件；阶段重新归属；名称加"副本"）
        if (includeFiles) {
            List<FileEntity> srcFiles = fileMapper.selectList(
                    new QueryWrapper<FileEntity>().eq("project_id", projectId).eq("deleted", 0));
            for (FileEntity f : srcFiles) {
                FileEntity n = new FileEntity();
                n.setProjectId(copy.getId());
                Long mappedStage = oldStageId2New.get(f.getStageId());
                n.setStageId(mappedStage == null ? 0L : mappedStage);
                n.setUploaderId(userId);
                n.setName(f.getName() + " (副本)");
                n.setUrl(f.getUrl());
                n.setSize(f.getSize());
                n.setType(f.getType());
                n.setStatus("pending");
                n.setVersion(1);
                n.setIsLatest(1);
                n.setParentId(0L);
                fileMapper.insert(n);
            }
        }

        // 记录操作日志
        OperationLog opLog = new OperationLog();
        opLog.setProjectId(copy.getId());
        opLog.setUserId(userId);
        opLog.setAction("copy_project");
        opLog.setTargetType("project");
        opLog.setTargetId(copy.getId());
        opLog.setDescription("从项目 [#" + projectId + "] 复制（tasks=" + includeTasks + ", files=" + includeFiles + "）");
        operationLogMapper.insert(opLog);

        ProjectVO vo = new ProjectVO();
        vo.setProjectId(copy.getId());
        vo.setGroupId(copy.getGroupId());
        vo.setName(copy.getName());
        vo.setStatus(copy.getStatus());
        vo.setProgress(copy.getProgress());
        vo.setCreatedAt(copy.getCreatedAt());
        return vo;
    }

    @Override
    @Transactional
    public void complete(Long projectId) {
        Project p = projectMapper.selectById(projectId);
        if (p == null) throw new BusinessException(ResultCode.PROJECT_NOT_FOUND.getCode());
        p.setStatus("completed");
        p.setProgress(100);
        projectMapper.updateById(p);
    }

    @Override
    public Map<String, Object> progress(Long projectId) {
        Project p = projectMapper.selectById(projectId);
        Map<String, Object> result = new HashMap<>();
        result.put("overall", p == null ? 0 : p.getProgress());

        Map<String, Object> fileStats = new HashMap<>();
        Long totalFile = fileMapper.selectCount(
                new QueryWrapper<FileEntity>().eq("project_id", projectId).eq("deleted", 0));
        Long approvedFile = fileMapper.selectCount(
                new QueryWrapper<FileEntity>().eq("project_id", projectId).eq("deleted", 0).eq("status", "approved"));
        fileStats.put("total", totalFile == null ? 0L : totalFile);
        fileStats.put("approved", approvedFile == null ? 0L : approvedFile);
        result.put("fileStats", fileStats);

        Map<String, Object> taskStats = new HashMap<>();
        Long totalTask = taskMapper.selectCount(
                new QueryWrapper<Task>().eq("project_id", projectId).eq("deleted", 0));
        Long completedTask = taskMapper.selectCount(
                new QueryWrapper<Task>().eq("project_id", projectId).eq("deleted", 0).eq("status", "completed"));
        taskStats.put("total", totalTask == null ? 0L : totalTask);
        taskStats.put("completed", completedTask == null ? 0L : completedTask);
        result.put("taskStats", taskStats);
        return result;
    }

    @Override
    public List<ActivityVO> activities(Long projectId, String type, int pageNum, int pageSize) {
        QueryWrapper<OperationLog> qw = new QueryWrapper<OperationLog>()
                .eq("project_id", projectId).eq("deleted", 0).orderByDesc("created_at");
        if (type != null && !type.isEmpty()) qw.eq("target_type", type);
        List<OperationLog> list = operationLogMapper.selectList(qw);
        List<ActivityVO> result = new ArrayList<>();
        for (OperationLog o : list) {
            ActivityVO vo = new ActivityVO();
            vo.setId(o.getId());
            vo.setType(o.getTargetType());
            vo.setAction(o.getDescription() == null ? o.getAction() : o.getDescription());
            vo.setUserId(o.getUserId());
            User u = userMapper.selectById(o.getUserId());
            vo.setUserName(u == null ? "" : u.getNickname());
            vo.setTargetId(o.getTargetId());
            vo.setTargetName("");
            vo.setCreatedAt(o.getCreatedAt() == null ? "" : o.getCreatedAt().toString());
            result.add(vo);
        }
        return result;
    }

    @Override
    public Map<String, Object> taskStats(Long projectId) {
        Long total = taskMapper.selectCount(
                new QueryWrapper<Task>().eq("project_id", projectId).eq("deleted", 0));
        Long completed = taskMapper.selectCount(
                new QueryWrapper<Task>().eq("project_id", projectId).eq("deleted", 0).eq("status", "completed"));
        Long inProgress = taskMapper.selectCount(
                new QueryWrapper<Task>().eq("project_id", projectId).eq("deleted", 0).eq("status", "in_progress"));
        Long pending = taskMapper.selectCount(
                new QueryWrapper<Task>().eq("project_id", projectId).eq("deleted", 0).eq("status", "pending"));
        Map<String, Object> result = new HashMap<>();
        result.put("total", total == null ? 0L : total);
        result.put("completed", completed == null ? 0L : completed);
        result.put("in_progress", inProgress == null ? 0L : inProgress);
        result.put("pending", pending == null ? 0L : pending);
        return result;
    }

    @Override
    @Transactional
    public StageVO addStage(Long projectId, AddStageRequest request) {
        Stage s = new Stage();
        s.setProjectId(projectId);
        s.setName(request.getName() == null ? "新阶段" : request.getName());
        s.setOrderNum(request.getOrderNum() == null ? 0 : request.getOrderNum());
        s.setStatus("pending");
        s.setStartDate(request.getStartDate());
        s.setEndDate(request.getEndDate());
        stageMapper.insert(s);
        return stageDetail(projectId, s.getId());
    }

    @Override
    public List<StageVO> stageList(Long projectId) {
        List<Stage> list = stageMapper.selectList(
                new QueryWrapper<Stage>().eq("project_id", projectId).eq("deleted", 0).orderByAsc("order_num"));
        List<StageVO> result = new ArrayList<>();
        for (Stage s : list) {
            result.add(toStageVO(s));
        }
        return result;
    }

    private StageVO toStageVO(Stage s) {
        StageVO vo = new StageVO();
        vo.setStageId(s.getId());
        vo.setName(s.getName());
        vo.setOrderNum(s.getOrderNum());
        vo.setStatus(s.getStatus());
        vo.setStartDate(s.getStartDate());
        vo.setEndDate(s.getEndDate());
        vo.setFileCount((int) (long) fileMapper.selectCount(
                new QueryWrapper<FileEntity>().eq("stage_id", s.getId()).eq("deleted", 0)));
        vo.setTaskCount((int) (long) taskMapper.selectCount(
                new QueryWrapper<Task>().eq("stage_id", s.getId()).eq("deleted", 0)));
        Long completed = taskMapper.selectCount(
                new QueryWrapper<Task>().eq("stage_id", s.getId()).eq("deleted", 0).eq("status", "completed"));
        vo.setCompletedTaskCount(completed == null ? 0 : completed.intValue());
        return vo;
    }

    @Override
    @Transactional
    public void stageOrder(Long projectId, StageOrderRequest request) {
        if (request.getStageIds() == null) return;
        for (int i = 0; i < request.getStageIds().size(); i++) {
            Long sid = request.getStageIds().get(i);
            Stage s = stageMapper.selectById(sid);
            if (s != null) {
                s.setOrderNum(i + 1);
                stageMapper.updateById(s);
            }
        }
    }

    @Override
    public StageVO stageDetail(Long projectId, Long stageId) {
        Stage s = stageMapper.selectById(stageId);
        if (s == null) throw new BusinessException(ResultCode.NOT_FOUND, "阶段不存在");
        return toStageVO(s);
    }

    @Override
    public void updateStage(Long projectId, Long stageId, UpdateStageRequest request) {
        Stage s = stageMapper.selectById(stageId);
        if (s == null) throw new BusinessException(ResultCode.NOT_FOUND, "阶段不存在");
        if (request.getName() != null) s.setName(request.getName());
        if (request.getOrderNum() != null) s.setOrderNum(request.getOrderNum());
        if (request.getStartDate() != null) s.setStartDate(request.getStartDate());
        if (request.getEndDate() != null) s.setEndDate(request.getEndDate());
        stageMapper.updateById(s);
    }

    @Override
    @Transactional
    public void deleteStage(Long projectId, Long stageId) {
        Stage s = stageMapper.selectById(stageId);
        if (s == null) return;
        s.setDeleted(1);
        stageMapper.updateById(s);
    }

    @Override
    public List<ProjectTemplateVO> templateList() {
        List<StageTemplate> list = stageTemplateMapper.selectList(
                new QueryWrapper<StageTemplate>().eq("deleted", 0));
        List<ProjectTemplateVO> result = new ArrayList<>();
        for (StageTemplate t : list) {
            ProjectTemplateVO vo = new ProjectTemplateVO();
            vo.setTemplateId(t.getId());
            vo.setName(t.getName());
            vo.setDescription(t.getDescription());
            vo.setIsPublic(true);
            // 阶段数：解析 stages_json
            if (t.getStagesJson() != null) {
                int cnt = t.getStagesJson().split(",").length;
                vo.setStageCount(cnt);
            } else {
                vo.setStageCount(0);
            }
            result.add(vo);
        }
        return result;
    }

    @Override
    public ProjectTemplateVO templateDetail(Long templateId) {
        StageTemplate t = stageTemplateMapper.selectById(templateId);
        if (t == null) throw new BusinessException(ResultCode.NOT_FOUND, "模板不存在");
        ProjectTemplateVO vo = new ProjectTemplateVO();
        vo.setTemplateId(t.getId());
        vo.setName(t.getName());
        vo.setDescription(t.getDescription());
        vo.setIsPublic(true);
        vo.setStageCount(t.getStagesJson() == null ? 0 : t.getStagesJson().split(",").length);
        return vo;
    }

    @Override
    @Transactional
    public ProjectTemplateVO createTemplate(Long userId, CreateTemplateRequest request) {
        StageTemplate t = new StageTemplate();
        t.setName(request.getName() == null ? "新模板" : request.getName());
        t.setDescription(request.getDescription());
        t.setCreatedBy(userId);
        // 简化：直接存阶段名 JSON
        t.setStagesJson("[]");
        stageTemplateMapper.insert(t);
        ProjectTemplateVO vo = new ProjectTemplateVO();
        vo.setTemplateId(t.getId());
        vo.setName(t.getName());
        vo.setDescription(t.getDescription());
        vo.setIsPublic(request.getIsPublic());
        vo.setStageCount(0);
        return vo;
    }

    // ============== T-06 贡献度统计 ==============
    @Override
    public List<ContributionStatsVO> contributionStats(Long projectId) {
        Project p = projectMapper.selectById(projectId);
        if (p == null) return Collections.emptyList();

        // 拿项目所在组的全部成员
        List<com.rocgit.knife4j.entity.group.GroupMember> members = groupMemberMapper.selectList(
                new QueryWrapper<com.rocgit.knife4j.entity.group.GroupMember>()
                        .eq("group_id", p.getGroupId())
                        .eq("deleted", 0));
        if (members.isEmpty()) return Collections.emptyList();

        List<ContributionStatsVO> result = new ArrayList<>();
        for (com.rocgit.knife4j.entity.group.GroupMember gm : members) {
            Long uid = gm.getUserId();
            ContributionStatsVO vo = new ContributionStatsVO();
            vo.setUserId(uid);
            User u = userMapper.selectById(uid);
            vo.setNickname(u == null ? ("用户" + uid) : u.getNickname());

            // 文件上传
            int fileUploads = (int) (long) fileMapper.selectCount(
                    new QueryWrapper<FileEntity>()
                            .eq("project_id", projectId).eq("uploader_id", uid).eq("deleted", 0));

            // 文件审核（action=approve/reject）
            int fileAudits = (int) (long) fileAuditLogMapper.selectCount(
                    new QueryWrapper<FileAuditLog>()
                            .in("file_id",
                                    new QueryWrapper<FileEntity>().select("id").eq("project_id", projectId).eq("deleted", 0))
                            .eq("auditor_id", uid).eq("deleted", 0));

            // 文件下载（action=download）
            int fileDownloads = (int) (long) fileAuditLogMapper.selectCount(
                    new QueryWrapper<FileAuditLog>()
                            .in("file_id",
                                    new QueryWrapper<FileEntity>().select("id").eq("project_id", projectId).eq("deleted", 0))
                            .eq("auditor_id", uid)
                            .eq("action", "download")
                            .eq("deleted", 0));

            // 完成任务数
            int tasksCompleted = (int) (long) taskMapper.selectCount(
                    new QueryWrapper<Task>()
                            .eq("project_id", projectId).eq("assignee_id", uid)
                            .eq("status", "completed").eq("deleted", 0));

            // 逾期未完成任务数
            int overdueTasks = (int) (long) taskMapper.selectCount(
                    new QueryWrapper<Task>()
                            .eq("project_id", projectId).eq("assignee_id", uid)
                            .ne("status", "completed").eq("deleted", 0)
                            .isNotNull("deadline").lt("deadline", LocalDateTime.now()));

            // 积分：上传+5，审核+3，下载+1，完成+8，逾期-2
            int score = fileUploads * 5 + fileAudits * 3 + fileDownloads * 1 + tasksCompleted * 8 + overdueTasks * (-2);
            score = Math.max(0, score); // 最低 0 分

            vo.setScore(score);
            vo.setFileUploads(fileUploads);
            vo.setFileAudits(fileAudits);
            vo.setFileDownloads(fileDownloads);
            vo.setTasksCompleted(tasksCompleted);
            vo.setOverdueTasks(overdueTasks);
            // 最后活跃时间：取 task completed_at / file created_at / audit created_at 最晚值
            LocalDateTime lastActive = getLastActive(projectId, uid);
            vo.setLastActive(lastActive);
            result.add(vo);
        }

        // 按 score 降序排，赋 rank
        result.sort((a, b) -> Integer.compare(b.getScore(), a.getScore()));
        for (int i = 0; i < result.size(); i++) result.get(i).setRank(i + 1);
        return result;
    }

    /** 取成员在项目内的最近活跃时间 */
    private LocalDateTime getLastActive(Long projectId, Long userId) {
        LocalDateTime latest = null;
        // task completed_at
        Task latestTask = taskMapper.selectOne(
                new QueryWrapper<Task>()
                        .eq("project_id", projectId).eq("assignee_id", userId).eq("deleted", 0)
                        .isNotNull("completed_at")
                        .orderByDesc("completed_at").last("LIMIT 1"));
        if (latestTask != null && latestTask.getCompletedAt() != null) latest = latestTask.getCompletedAt();
        // file upload
        FileEntity latestFile = fileMapper.selectOne(
                new QueryWrapper<FileEntity>()
                        .eq("project_id", projectId).eq("uploader_id", userId).eq("deleted", 0)
                        .orderByDesc("created_at").last("LIMIT 1"));
        if (latestFile != null && latestFile.getCreatedAt() != null) {
            if (latest == null || latestFile.getCreatedAt().isAfter(latest)) latest = latestFile.getCreatedAt();
        }
        // audit log
        FileAuditLog latestLog = fileAuditLogMapper.selectOne(
                new QueryWrapper<FileAuditLog>()
                        .in("file_id", new QueryWrapper<FileEntity>().select("id").eq("project_id", projectId).eq("deleted", 0))
                        .eq("auditor_id", userId).eq("deleted", 0)
                        .orderByDesc("created_at").last("LIMIT 1"));
        if (latestLog != null && latestLog.getCreatedAt() != null) {
            if (latest == null || latestLog.getCreatedAt().isAfter(latest)) latest = latestLog.getCreatedAt();
        }
        return latest;
    }
}