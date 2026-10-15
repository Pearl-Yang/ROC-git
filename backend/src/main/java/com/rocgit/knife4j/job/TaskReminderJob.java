package com.rocgit.knife4j.job;

import com.rocgit.knife4j.service.project.MilestoneReminderService;
import com.rocgit.knife4j.service.task.TaskReminderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时任务：每 60s 跑一次
 *  1) TaskReminderService.scanAndCreate() —— 扫未完成任务，补建 before_due/overdue 提醒
 *  2) TaskReminderService.sendDueReminders() —— 把到点的提醒推成 message
 *  3) MilestoneReminderService.scan() —— T-07：项目里程碑（阶段截止日）提醒
 */
@Component
public class TaskReminderJob {

    private static final Logger log = LoggerFactory.getLogger(TaskReminderJob.class);

    @Autowired
    private TaskReminderService reminderService;
    @Autowired
    private MilestoneReminderService milestoneService;

    @Scheduled(initialDelay = 30_000, fixedDelay = 60_000)
    public void tick() {
        try {
            int created = reminderService.scanAndCreate();
            int sent = reminderService.sendDueReminders();
            int milestoneSent = milestoneService.scan();
            if (created > 0 || sent > 0 || milestoneSent > 0) {
                log.info("[TaskReminderJob] 本轮扫描：新建 {} 条任务提醒，已推送 {} 条任务消息，{} 条里程碑",
                        created, sent, milestoneSent);
            }
        } catch (Exception e) {
            log.warn("[TaskReminderJob] 本轮扫描异常：{}", e.getMessage());
        }
    }
}
