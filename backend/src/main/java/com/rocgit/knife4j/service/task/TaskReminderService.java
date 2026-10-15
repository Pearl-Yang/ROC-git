package com.rocgit.knife4j.service.task;

import com.rocgit.knife4j.entity.task.TaskReminderVO;

import java.util.List;

public interface TaskReminderService {

    /**
     * 扫描所有未完成任务，按 deadline 自动生成 before_due / overdue 提醒（每个任务每种类型最多一条）。
     */
    int scanAndCreate();

    /**
     * 把 is_sent=0 且 remind_at <= now 的提醒转成 message 记录，并标记 is_sent=1。
     * @return 实际发出的提醒数
     */
    int sendDueReminders();

    /**
     * 在分配任务时（TaskServiceImpl.create）立刻生成一条 assign 类型提醒。
     */
    void createAssignReminder(Long taskId, Long assigneeId, String taskTitle);

    /**
     * 任务完成后，关闭未发 before_due 提醒，避免任务完成还继续催。
     */
    void cancelPendingRemindersForTask(Long taskId);

    /**
     * 当前用户的提醒列表（按时间倒序）
     */
    List<TaskReminderVO> listMine(Long userId, int pageNum, int pageSize);

    /**
     * 当前用户未发送的提醒数（用于"消息 tab 角标"）
     */
    int countUnsentMine(Long userId);
}
