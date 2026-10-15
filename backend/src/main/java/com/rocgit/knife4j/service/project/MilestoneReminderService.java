package com.rocgit.knife4j.service.project;

import com.rocgit.knife4j.entity.Project;
import com.rocgit.knife4j.entity.message.Message;
import com.rocgit.knife4j.entity.stage.Stage;
import com.rocgit.knife4j.mapper.ProjectMapper;
import com.rocgit.knife4j.mapper.group.GroupMemberMapper;
import com.rocgit.knife4j.mapper.message.MessageMapper;
import com.rocgit.knife4j.mapper.stage.StageMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * T-07：项目里程碑（stage.end_date）日期提醒。
 * 策略：项目阶段截止日 ≤ 7 天内 或 已逾期，给项目小组成员发"项目里程碑"消息。
 * 去重：同一 stage 的同种类型当天不重复发。
 */
@Service
public class MilestoneReminderService {

    private static final Logger log = LoggerFactory.getLogger(MilestoneReminderService.class);

    @Autowired private ProjectMapper projectMapper;
    @Autowired private StageMapper stageMapper;
    @Autowired private GroupMemberMapper groupMemberMapper;
    @Autowired private MessageMapper messageMapper;

    @Transactional
    public int scan() {
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();
        int sent = 0;
        // 1) 即将到期：未来 7 天内
        List<Stage> stages = stageMapper.selectList(
                new QueryWrapper<Stage>()
                        .isNotNull("end_date")
                        .ge("end_date", today)
                        .le("end_date", today.plusDays(7))
                        .ne("status", "completed")
                        .eq("deleted", 0));
        for (Stage s : stages) {
            sent += pushMilestone(s, "milestone_upcoming", now);
        }
        // 2) 已逾期
        List<Stage> overdue = stageMapper.selectList(
                new QueryWrapper<Stage>()
                        .isNotNull("end_date")
                        .lt("end_date", today)
                        .ne("status", "completed")
                        .eq("deleted", 0));
        for (Stage s : overdue) {
            sent += pushMilestone(s, "milestone_overdue", now);
        }
        if (sent > 0) log.info("[MilestoneReminder] 已发送 {} 条里程碑提醒", sent);
        return sent;
    }

    private int pushMilestone(Stage s, String type, LocalDateTime now) {
        Project p = projectMapper.selectById(s.getProjectId());
        if (p == null || p.getDeleted() != null && p.getDeleted() == 1) return 0;
        // 取项目小组成员
        List<com.rocgit.knife4j.entity.group.GroupMember> members = groupMemberMapper.selectList(
                new QueryWrapper<com.rocgit.knife4j.entity.group.GroupMember>()
                        .eq("group_id", p.getGroupId()).eq("deleted", 0));
        if (members.isEmpty()) return 0;
        Set<Long> dedup = new HashSet<>();
        int sent = 0;
        for (com.rocgit.knife4j.entity.group.GroupMember gm : members) {
            Long uid = gm.getUserId();
            if (dedup.contains(uid)) continue;
            dedup.add(uid);
            // 当天去重：同 user + relatedType=stage + relatedId=stageId + type 已有记录则跳过
            Long dupCount = messageMapper.selectCount(
                    new QueryWrapper<Message>()
                            .eq("user_id", uid)
                            .eq("type", type)
                            .eq("related_type", "stage")
                            .eq("related_id", s.getId())
                            .ge("created_at", now.toLocalDate().atStartOfDay()));
            if (dupCount != null && dupCount > 0) continue;
            Message m = new Message();
            m.setUserId(uid);
            if ("milestone_overdue".equals(type)) {
                m.setTitle("项目里程碑已逾期：「" + s.getName() + "」");
                m.setContent("项目「" + (p.getName() == null ? "" : p.getName()) + "」的阶段「" + s.getName()
                        + "」已于 " + s.getEndDate() + " 截止，请尽快推进。");
            } else {
                m.setTitle("项目里程碑即将到期：「" + s.getName() + "」");
                m.setContent("项目「" + (p.getName() == null ? "" : p.getName()) + "」的阶段「" + s.getName()
                        + "」将于 " + s.getEndDate() + " 截止，距离现在还有 "
                        + java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), s.getEndDate()) + " 天。");
            }
            m.setType(type);
            m.setRelatedType("stage");
            m.setRelatedId(s.getId());
            m.setIsRead(0);
            messageMapper.insert(m);
            sent++;
        }
        return sent;
    }
}
