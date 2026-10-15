package com.rocgit.knife4j.service.task.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.rocgit.knife4j.common.BusinessException;
import com.rocgit.knife4j.common.ResultCode;
import com.rocgit.knife4j.dto.task.*;
import com.rocgit.knife4j.entity.task.Task;
import com.rocgit.knife4j.entity.task.TaskVO;
import com.rocgit.knife4j.mapper.task.TaskMapper;
import com.rocgit.knife4j.service.task.TaskReminderService;
import com.rocgit.knife4j.service.task.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class TaskServiceImpl implements TaskService {

    @Autowired private TaskMapper taskMapper;
    @Autowired private TaskReminderService taskReminderService; // T-09：分配任务时立刻生成 assign 提醒

    @Override
    @Transactional
    public TaskVO create(Long userId, CreateTaskRequest request) {
        Task t = new Task();
        t.setProjectId(request.getProjectId());
        t.setStageId(request.getStageId() == null ? 0L : request.getStageId());
        t.setTitle(request.getName() == null ? "" : request.getName());
        t.setDescription(request.getDescription());
        t.setAssigneeId(request.getAssigneeId() == null ? userId : request.getAssigneeId());
        t.setStatus("pending");
        t.setDeadline(request.getDeadline());
        taskMapper.insert(t);

        // T-09：分配任务立刻生成 assign 提醒 + 推送消息
        try {
            taskReminderService.createAssignReminder(t.getId(), t.getAssigneeId(), t.getTitle());
        } catch (Exception ignore) { /* 不影响主流程 */ }

        return toVO(t);
    }

    @Override
    public Map<String, Object> list(Long projectId, Long assigneeId, String status) {
        QueryWrapper<Task> qw = new QueryWrapper<Task>().eq("project_id", projectId).eq("deleted", 0);
        if (assigneeId != null) qw.eq("assignee_id", assigneeId);
        if (status != null && !status.isEmpty()) qw.eq("status", status);
        qw.orderByDesc("created_at");
        List<Task> list = taskMapper.selectList(qw);
        List<TaskVO> vos = new ArrayList<>();
        for (Task t : list) vos.add(toVO(t));
        Map<String, Object> result = new HashMap<>();
        result.put("total", (long) vos.size());
        result.put("list", vos);
        return result;
    }

    @Override
    public Map<String, Object> myTasks(Long userId, String status, Boolean overdue, int pageNum, int pageSize) {
        QueryWrapper<Task> qw = new QueryWrapper<Task>().eq("assignee_id", userId).eq("deleted", 0);
        if (status != null && !status.isEmpty()) qw.eq("status", status);
        if (Boolean.TRUE.equals(overdue)) {
            qw.lt("deadline", LocalDateTime.now()).ne("status", "completed");
        }
        qw.orderByDesc("created_at");
        int offset = Math.max(0, (pageNum - 1) * pageSize);
        qw.last("LIMIT " + offset + "," + pageSize);
        List<Task> list = taskMapper.selectList(qw);
        Long total = taskMapper.selectCount(new QueryWrapper<Task>().eq("assignee_id", userId).eq("deleted", 0));
        List<TaskVO> vos = new ArrayList<>();
        for (Task t : list) vos.add(toVO(t));
        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("list", vos);
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);
        return result;
    }

    @Override
    public List<TaskVO> dueSoon(int days) {
        LocalDateTime end = LocalDateTime.now().plusDays(days);
        List<Task> list = taskMapper.selectList(
                new QueryWrapper<Task>()
                        .isNotNull("deadline")
                        .le("deadline", end)
                        .ne("status", "completed")
                        .eq("deleted", 0)
                        .orderByAsc("deadline")
                        .last("LIMIT 50"));
        List<TaskVO> result = new ArrayList<>();
        for (Task t : list) result.add(toVO(t));
        return result;
    }

    @Override
    public TaskVO detail(Long taskId) {
        Task t = taskMapper.selectById(taskId);
        if (t == null || (t.getDeleted() != null && t.getDeleted() == 1)) {
            throw new BusinessException(ResultCode.TASK_NOT_FOUND, "任务不存在");
        }
        return toVO(t);
    }

    @Override
    public void update(Long taskId, UpdateTaskRequest request) {
        Task t = taskMapper.selectById(taskId);
        if (t == null) throw new BusinessException(ResultCode.TASK_NOT_FOUND, "任务不存在");
        if (request.getName() != null) t.setTitle(request.getName());
        if (request.getDescription() != null) t.setDescription(request.getDescription());
        if (request.getAssigneeId() != null) t.setAssigneeId(request.getAssigneeId());
        if (request.getStageId() != null) t.setStageId(request.getStageId());
        if (request.getDeadline() != null) t.setDeadline(request.getDeadline());
        if (request.getStatus() != null) t.setStatus(request.getStatus());
        taskMapper.updateById(t);
    }

    @Override
    @Transactional
    public void delete(Long taskId) {
        Task t = taskMapper.selectById(taskId);
        if (t == null) return;
        t.setDeleted(1);
        taskMapper.updateById(t);
    }

    @Override
    @Transactional
    public void complete(Long taskId, CompleteTaskRequest request) {
        Task t = taskMapper.selectById(taskId);
        if (t == null) throw new BusinessException(ResultCode.TASK_NOT_FOUND, "任务不存在");
        t.setStatus("completed");
        t.setCompletedAt(LocalDateTime.now());
        taskMapper.updateById(t);
        // T-09：任务完成，关闭未发的 before_due 提醒，避免任务完成还继续催
        try { taskReminderService.cancelPendingRemindersForTask(taskId); } catch (Exception ignore) {}
    }

    @Override
    @Transactional
    public void abandon(Long taskId, AbandonTaskRequest request) {
        Task t = taskMapper.selectById(taskId);
        if (t == null) throw new BusinessException(ResultCode.TASK_NOT_FOUND, "任务不存在");
        t.setStatus("abandoned");
        taskMapper.updateById(t);
    }

    @Override
    @Transactional
    public void transfer(Long taskId, TransferTaskRequest request) {
        Task t = taskMapper.selectById(taskId);
        if (t == null) throw new BusinessException(ResultCode.TASK_NOT_FOUND, "任务不存在");
        if (request.getNewAssigneeId() != null) t.setAssigneeId(request.getNewAssigneeId());
        taskMapper.updateById(t);
    }

    @Override
    @Transactional
    public void updateProgress(Long taskId, TaskProgressRequest request) {
        Task t = taskMapper.selectById(taskId);
        if (t == null) throw new BusinessException(ResultCode.TASK_NOT_FOUND, "任务不存在");
        if (request != null && request.getProgress() != null) {
            int p = Math.max(0, Math.min(100, request.getProgress()));
            t.setProgress(p);
            // 进度到 100 自动完成
            if (p >= 100) {
                if (!"completed".equalsIgnoreCase(t.getStatus())) {
                    t.setStatus("completed");
                    t.setCompletedAt(LocalDateTime.now());
                }
            } else if (p > 0) {
                if ("pending".equalsIgnoreCase(t.getStatus())) t.setStatus("in_progress");
            } else {
                // 进度回退到 0：如果之前是 completed，则回退为 in_progress
                if ("completed".equalsIgnoreCase(t.getStatus())) {
                    t.setStatus("in_progress");
                    t.setCompletedAt(null);
                }
            }
        }
        taskMapper.updateById(t);
        // 同步关闭/恢复 before_due 提醒
        if (t.getStatus() != null && t.getStatus().equalsIgnoreCase("completed")) {
            try { taskReminderService.cancelPendingRemindersForTask(taskId); } catch (Exception ignore) {}
        }
    }

    private TaskVO toVO(Task t) {
        TaskVO vo = new TaskVO();
        vo.setTaskId(t.getId());
        vo.setProjectId(t.getProjectId());
        vo.setStageId(t.getStageId());
        vo.setTitle(t.getTitle());
        vo.setName(t.getTitle());
        vo.setDescription(t.getDescription());
        vo.setAssigneeId(t.getAssigneeId());
        vo.setStatus(t.getStatus());
        vo.setProgress(t.getProgress());
        vo.setDeadline(t.getDeadline());
        vo.setCompletedAt(t.getCompletedAt());
        vo.setCreatedAt(t.getCreatedAt());
        return vo;
    }
}
