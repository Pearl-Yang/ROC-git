package com.rocgit.knife4j.task;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.rocgit.knife4j.entity.file.FileEntity;
import com.rocgit.knife4j.mapper.file.FileMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * W3 · T-10 文件锁过期清理任务
 *
 * <p>每分钟扫描一次，把 lock_expire_at < now 的记录清空（lock_user_id=0, expire=null）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FileLockExpireTask {

    private final FileMapper fileMapper;

    @Scheduled(fixedDelay = 60_000L, initialDelay = 30_000L)
    @Transactional
    public void clearExpiredLocks() {
        UpdateWrapper<FileEntity> uw = new UpdateWrapper<>();
        uw.set("lock_user_id", 0)
          .set("lock_expire_at", null)
          .gt("lock_user_id", 0)
          .lt("lock_expire_at", LocalDateTime.now());
        int n = fileMapper.update(null, uw);
        if (n > 0) {
            log.info("[FileLockExpire] 清理 {} 条过期锁", n);
        }
    }
}
