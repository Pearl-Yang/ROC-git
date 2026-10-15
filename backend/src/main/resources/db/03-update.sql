-- ==========================================================
-- 03-update.sql  历史变更（请在更新时执行）
-- ==========================================================

-- T-08: 任务进度（0-100）
ALTER TABLE `task`
  ADD COLUMN `progress` TINYINT NOT NULL DEFAULT 0
  COMMENT '任务进度 0-100，100=completed' AFTER `status`;

-- T-10: 文件锁（防并发上传同名）
ALTER TABLE `file`
  ADD COLUMN `lock_user_id`   BIGINT       NOT NULL DEFAULT 0
  COMMENT '锁定人ID（0=未锁）' AFTER `deleted`,
  ADD COLUMN `lock_expire_at` DATETIME     NULL DEFAULT NULL
  COMMENT '锁过期时间' AFTER `lock_user_id`,
  ADD UNIQUE KEY `uk_project_stage_name_latest`
    (`project_id`, `stage_id`, `name`, `is_latest`);
