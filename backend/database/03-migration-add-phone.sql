-- ============================================================
-- T-25 手机号+验证码登录 增量迁移脚本
-- 适用：生产已部署、user 表已有数据的场景
-- 用法：mysql -uroot -p roc_git < 03-migration-add-phone.sql
--       （docker 环境：docker exec -i rocgit-mysql mysql -uroot -proot roc_git < 03-migration-add-phone.sql）
-- 字符集：utf8mb4
-- ============================================================

-- 1) user 表追加 phone 字段（手机号验证码登录主键）
ALTER TABLE `user`
  ADD COLUMN `phone` VARCHAR(11) NOT NULL DEFAULT '' COMMENT '手机号（11位）' AFTER `avatar_url`;

-- 2) 加唯一索引
ALTER TABLE `user`
  ADD UNIQUE KEY `uk_user_phone` (`phone`);

-- 3) 创建短信验证码表（T-25）
CREATE TABLE IF NOT EXISTS `user_sms_verify_code` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT,
  `phone`      VARCHAR(11)  NOT NULL DEFAULT '' COMMENT '手机号',
  `code`       VARCHAR(6)   NOT NULL DEFAULT '' COMMENT '6位验证码',
  `purpose`    VARCHAR(16)  NOT NULL DEFAULT 'login' COMMENT '用途：login / reset',
  `expire_at`  DATETIME     NOT NULL COMMENT '过期时间',
  `used`       TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已用',
  `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`    TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_phone_purpose` (`phone`, `purpose`, `used`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='短信验证码表';
