package com.rocgit.knife4j.task;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.rocgit.knife4j.entity.User;
import com.rocgit.knife4j.entity.user.LoginHistory;
import com.rocgit.knife4j.mapper.UserMapper;
import com.rocgit.knife4j.mapper.user.LoginHistoryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * P3-5 隐私合规定时任务
 *
 * <ol>
 *   <li>凌晨 03:00 物理删除 30 天前注销的账号</li>
 *   <li>凌晨 03:10 把 30 天前的 login_history.ip 脱敏为 0.0.0.0</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PrivacyComplianceTask {

    private final UserMapper userMapper;
    private final LoginHistoryMapper loginHistoryMapper;

    /** 凌晨 03:00：物理删除 30 天前注销的账号 */
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void purgeCanceledAccounts() {
        Date now = new Date();
        List<User> due = userMapper.selectList(
                new QueryWrapper<User>().eq("deleted", 1).lt("purge_at", now));
        if (due.isEmpty()) {
            log.info("[PrivacyCompliance] 暂无待物理删除的注销账号");
            return;
        }
        for (User u : due) {
            // 实际生产应该把 user_* / project_member / file 等多表一并脱关联；这里先删除 user 主表
            userMapper.deleteById(u.getId());
            log.warn("[PrivacyCompliance] 物理删除 user id={} phone={}", u.getId(), u.getPhone());
        }
    }

    /** 凌晨 03:10：30 天前的 ip 字段脱敏 */
    @Scheduled(cron = "0 10 3 * * ?")
    public void anonymizeLoginIps() {
        Date cutoff = new Date(System.currentTimeMillis() - 30L * 24 * 3600 * 1000);
        List<LoginHistory> outdated = loginHistoryMapper.selectList(
                new QueryWrapper<LoginHistory>().lt("login_at", cutoff).ne("ip", "0.0.0.0"));
        if (outdated.isEmpty()) {
            log.info("[PrivacyCompliance] 暂无待脱敏的 IP 记录");
            return;
        }
        for (LoginHistory h : outdated) {
            h.setIp("0.0.0.0");
            loginHistoryMapper.updateById(h);
        }
        log.info("[PrivacyCompliance] 脱敏 {} 条 30 天前的 IP 记录", outdated.size());
    }
}
