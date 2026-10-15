package com.rocgit.knife4j.service.task.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.rocgit.knife4j.entity.Project;
import com.rocgit.knife4j.entity.message.Message;
import com.rocgit.knife4j.entity.task.Task;
import com.rocgit.knife4j.entity.task.TaskReminder;
import com.rocgit.knife4j.entity.task.TaskReminderVO;
import com.rocgit.knife4j.mapper.ProjectMapper;
import com.rocgit.knife4j.mapper.message.MessageMapper;
import com.rocgit.knife4j.mapper.task.TaskMapper;
import com.rocgit.knife4j.mapper.task.TaskReminderMapper;
import com.rocgit.knife4j.service.task.TaskReminderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * T-09 任务提醒服务
 *
 * 数据流：
 *   1) TaskServiceImpl.create → createAssignReminder → 立刻写一条 assign 提醒
 *   2) @Scheduled TaskReminderJob.tick() → scanAndCreate()  → 扫所有未完成任务，
 *      对每条 task 检查 (deadline-1day before_due) 和 (deadline+1day overdue) 是否已存在，
 *      缺失则补建 reminder。
 *   3) @Scheduled tick() → sendDueReminders() → 把 is_sent=0 且 remind_at <= now 的 reminder
 *      转成 message（userId=task.assigneeId），并标记 is_sent=1。
 *   4) TaskServiceImpl.complete → cancelPendingRemindersForTask → 把该 task 的未发 before_due 标记 is_sent=1
 */
@Service
public class TaskReminderServiceImpl implements TaskReminderService {

    private static final Logger log = LoggerFactory.getLogger(TaskReminderServiceImpl.class);

    @Autowired private TaskMapper taskMapper;
    @Autowired private TaskReminderMapper reminderMapper;
    @Autowired private MessageMapper messageMapper;
    @Autowired private ProjectMapper projectMapper;

    @Override
    @Transactional
    public int scanAndCreate() {
        // 1) 拉所有未完成且有 deadline 的任务
        List<Task> tasks = taskMapper.selectList(
                new QueryWrapper<Task>()
                        .isNotNull("deadline")
                        .ne("status", "completed")
                        .ne("status", "abandoned")
                        .eq("deleted", 0));
        if (tasks.isEmpty()) return 0;
        LocalDateTime now = LocalDateTime.now();
        int created = 0;

        for (Task t : tasks) {
            if (t.getDeadline() == null) continue;
            LocalDateTime due = t.getDeadline();

            // before_due：deadline - 24h
            LocalDateTime beforeAt = due.minusHours(24);
            if (!hasReminder(t.getId(), "before_due")) {
                if (beforeAt.isAfter(now.minusMinutes(1))) {
                    // 还没到提醒时间则不创建（避免提前催）
                } else {
                    insertReminder(t.getId(), "before_due", beforeAt);
                    created++;
                }
            }

            // overdue：deadline + 24h
            LocalDateTime overAt = due.plusHours(24);
            if (!hasReminder(t.getId(), "overdue")) {
                insertReminder(t.getId(), "overdue", overAt);
                created++;
            }
        }
        if (created > 0) log.info("[TaskReminder] scanAndCreate 补建 {} 条提醒", created);
        return created;
    }

    @Override
    @Transactional
    public int sendDueReminders() {
        LocalDateTime now = LocalDateTime.now();
        List<TaskReminder> pending = reminderMapper.selectList(
                new QueryWrapper<TaskReminder>().eq("is_sent", 0).eq("deleted", 0).le("remind_at", now));
        if (pending.isEmpty()) return 0;
        int sent = 0;
        for (TaskReminder r : pending) {
            Task t = taskMapper.selectById(r.getTaskId());
            if (t == null || t.getDeleted() == 1) {
                r.setIsSent(1);
                r.setUpdatedAt(now);
                reminderMapper.updateById(r);
                continue;
            }
            // 任务已完成 → 不再发
            if ("completed".equalsIgnoreCase(t.getStatus()) || "abandoned".equalsIgnoreCase(t.getStatus())) {
                r.setIsSent(1);
                r.setUpdatedAt(now);
                reminderMapper.updateById(r);
                continue;
            }
            // 没有负责人 → 也跳过
            if (t.getAssigneeId() == null || t.getAssigneeId() <= 0) {
                r.setIsSent(1);
                r.setUpdatedAt(now);
                reminderMapper.updateById(r);
                continue;
            }
            Message m = new Message();
            m.setUserId(t.getAssigneeId());
            m.setTitle(buildTitle(r.getReminderType(), t.getTitle()));
            m.setContent(buildContent(r.getReminderType(), t));
            m.setType("task_reminder");
            m.setRelatedType("task");
            m.setRelatedId(t.getId());
            m.setIsRead(0);
            messageMapper.insert(m);

            r.setIsSent(1);
            r.setUpdatedAt(now);
            reminderMapper.updateById(r);
            sent++;
        }
        if (sent > 0) log.info("[TaskReminder] sendDueReminders 已推送 {} 条消息", sent);
        return sent;
    }

    @Override
    @Transactional
    public void createAssignReminder(Long taskId, Long assigneeId, String taskTitle) {
        if (taskId == null || assigneeId == null) return;
        if (hasReminder(taskId, "assign")) return;
        insertReminder(taskId, "assign", LocalDateTime.now());
        // assign 类型立刻发送一条
        Message m = new Message();
        m.setUserId(assigneeId);
        m.setTitle("您有新任务");
        m.setContent("任务「" + (taskTitle == null ? "" : taskTitle) + "」已分配给您，请尽快查看。");
        m.setType("task_reminder");
        m.setRelatedType("task");
        m.setRelatedId(taskId);
        m.setIsRead(0);
        messageMapper.insert(m);
        // 把 reminder 也标 is_sent
        TaskReminder r = reminderMapper.selectOne(
                new QueryWrapper<TaskReminder>().eq("task_id", taskId).eq("reminder_type", "assign").eq("deleted", 0).last("LIMIT 1"));
        if (r != null) {
            r.setIsSent(1);
            reminderMapper.updateById(r);
        }
    }

    @Override
    @Transactional
    public void cancelPendingRemindersForTask(Long taskId) {
        if (taskId == null) return;
        TaskReminder update = new TaskReminder();
        update.setIsSent(1);
        update.setUpdatedAt(LocalDateTime.now());
        reminderMapper.update(update,
                new UpdateWrapper<TaskReminder>()
                        .eq("task_id", taskId)
                        .eq("is_sent", 0)
                        .eq("deleted", 0));
    }

    @Override
    public List<TaskReminderVO> listMine(Long userId, int pageNum, int pageSize) {
        // 思路：reminder 表没 userId 字段；改用 message.type=task_reminder + message.user_id=自己 + related_id=task.id
        // 这里返的是已发消息转成的提醒展示（含任务标题、项目名）
        int offset = Math.max(0, (pageNum - 1) * pageSize);
        List<Message> msgs = messageMapper.selectList(
                new QueryWrapper<Message>()
                        .eq("user_id", userId)
                        .eq("type", "task_reminder")
                        .eq("deleted", 0)
                        .orderByDesc("created_at")
                        .last("LIMIT " + offset + "," + pageSize));
        List<TaskReminderVO> result = new ArrayList<>();
        for (Message m : msgs) {
            TaskReminderVO vo = new TaskReminderVO();
            vo.setId(m.getId());
            vo.setTaskId(m.getRelatedId());
            // 关联查 task + project
            if (m.getRelatedId() != null) {
                Task t = taskMapper.selectById(m.getRelatedId());
                if (t != null) {
                    vo.setTaskTitle(t.getTitle());
                    vo.setProjectId(t.getProjectId());
                    if (t.getProjectId() != null) {
                        Project p = projectMapper.selectById(t.getProjectId());
                        if (p != null) vo.setProjectName(p.getName());
                    }
                }
            }
            // 从 title 反推 reminderType：含「即将到期」=before_due；含「已逾期」=overdue；含「新任务」=assign
            String t = m.getTitle() == null ? "" : m.getTitle();
            if (t.contains("即将到期")) vo.setReminderType("before_due");
            else if (t.contains("已逾期")) vo.setReminderType("overdue");
            else if (t.contains("新任务")) vo.setReminderType("assign");
            else vo.setReminderType("other");
            vo.setMessage(m.getContent());
            vo.setIsSent(1);
            vo.setRemindAt(m.getCreatedAt());
            vo.setSentAt(m.getCreatedAt());
            result.add(vo);
        }
        return result;
    }

    @Override
    public int countUnsentMine(Long userId) {
        // 等价于未读 task_reminder 类型消息数
        Long c = messageMapper.selectCount(
                new QueryWrapper<Message>()
                        .eq("user_id", userId)
                        .eq("type", "task_reminder")
                        .eq("is_read", 0)
                        .eq("deleted", 0));
        return c == null ? 0 : c.intValue();
    }

    // ============== 内部工具 ==============

    private boolean hasReminder(Long taskId, String type) {
        if (taskId == null) return false;
        Long c = reminderMapper.selectCount(
                new QueryWrapper<TaskReminder>().eq("task_id", taskId).eq("reminder_type", type).eq("deleted", 0));
        return c != null && c > 0;
    }

    private void insertReminder(Long taskId, String type, LocalDateTime remindAt) {
        TaskReminder r = new TaskReminder();
        r.setTaskId(taskId);
        r.setReminderType(type);
        r.setRemindAt(remindAt == null ? LocalDateTime.now() : remindAt);
        r.setIsSent(0);
        reminderMapper.insert(r);
    }

    private String buildTitle(String type, String taskTitle) {
        if ("before_due".equals(type)) return "任务即将到期：「" + (taskTitle == null ? "" : taskTitle) + "」";
        if ("overdue".equals(type))    return "任务已逾期：「" + (taskTitle == null ? "" : taskTitle) + "」";
        if ("assign".equals(type))     return "您有新任务：「" + (taskTitle == null ? "" : taskTitle) + "」";
        return "任务提醒：「" + (taskTitle == null ? "" : taskTitle) + "」";
    }

    private String buildContent(String type, Task t) {
        if ("before_due".equals(type)) {
            return "截止时间：" + t.getDeadline() + "，请尽快完成。";
        }
        if ("overdue".equals(type)) {
            return "已于 " + t.getDeadline() + " 截止，请尽快处理。";
        }
        return "请查收。";
    }
}
