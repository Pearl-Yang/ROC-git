-- ============================================================
-- ROC-git 课程小组文档协作管理平台 · MySQL 8.0 Schema
-- 文件：db/schema.sql
-- 用途：Spring Boot 启动时自动执行（spring.sql.init.mode=always）
-- 库：roc_git（由 docker-compose 的 MYSQL_DATABASE 提前建好）
-- 字符集：utf8mb4_unicode_ci，引擎：InnoDB
-- 字段依据：
--   §8.1 数据字典（user / group / group_member / project / stage /
--   task / file / operation_log / project_share）
--   §5 类图（其余 13 张表 + 修正 §8.1 未列出的子表）
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 一、核心表（按 §8.1 数据字典 + §5.1 核心类图）
-- ============================================================

-- 1. user 用户表（§8.1.1：纯微信登录 + 手机号验证码登录）
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `openid`     VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '微信OpenID',
  `nickname`   VARCHAR(32)  NOT NULL DEFAULT '' COMMENT '微信昵称',
  `avatar_url` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '头像URL',
  `phone`      VARCHAR(11)  NOT NULL DEFAULT '' COMMENT '手机号（手机号验证码登录主键）',
  `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`    TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_openid` (`openid`),
  UNIQUE KEY `uk_user_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 2. group 小组表（§8.1.2）
DROP TABLE IF EXISTS `group`;
CREATE TABLE `group` (
  `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '小组ID',
  `name`        VARCHAR(50) NOT NULL DEFAULT '' COMMENT '小组名称',
  `leader_id`   BIGINT      NOT NULL DEFAULT 0 COMMENT '组长ID',
  `invite_code` VARCHAR(8)  NOT NULL DEFAULT '' COMMENT '邀请码',
  `status`      VARCHAR(16) NOT NULL DEFAULT 'active' COMMENT 'active/archived',
  `created_at`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`     TINYINT     NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_invite_code` (`invite_code`),
  KEY `idx_leader_id` (`leader_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='小组表';

-- 3. group_member 小组成员表（§8.1.3）
DROP TABLE IF EXISTS `group_member`;
CREATE TABLE `group_member` (
  `id`        BIGINT      NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `group_id`  BIGINT      NOT NULL DEFAULT 0 COMMENT '小组ID',
  `user_id`   BIGINT      NOT NULL DEFAULT 0 COMMENT '用户ID',
  `role`      VARCHAR(16) NOT NULL DEFAULT 'member' COMMENT 'leader/member',
  `status`    VARCHAR(16) NOT NULL DEFAULT 'active' COMMENT 'active/inviting/removed',
  `joined_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`   TINYINT     NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_group_user` (`group_id`, `user_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='小组成员表';

-- 4. project 项目表（§8.1.4）
DROP TABLE IF EXISTS `project`;
CREATE TABLE `project` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '项目ID',
  `group_id`      BIGINT       NOT NULL DEFAULT 0 COMMENT '小组ID',
  `name`          VARCHAR(100) NOT NULL DEFAULT '' COMMENT '项目名称',
  `description`   TEXT         NULL COMMENT '项目描述',
  `current_stage` VARCHAR(50)  NOT NULL DEFAULT '' COMMENT '当前阶段',
  `progress`      INT          NOT NULL DEFAULT 0 COMMENT '进度%',
  `status`        VARCHAR(16)  NOT NULL DEFAULT 'active' COMMENT 'active/completed/archived',
  `is_shared`     TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已分享',
  `share_token`   VARCHAR(32)  NOT NULL DEFAULT '' COMMENT '分享Token',
  `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `ended_at`      DATETIME     NULL DEFAULT NULL COMMENT '结束时间',
  `shared_at`     DATETIME     NULL DEFAULT NULL COMMENT '分享时间',
  `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`       TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_group_id` (`group_id`),
  KEY `idx_status` (`status`),
  KEY `idx_share_token` (`share_token`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='项目表';

-- 5. stage 阶段表（§8.1.5）
DROP TABLE IF EXISTS `stage`;
CREATE TABLE `stage` (
  `id`         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '阶段ID',
  `project_id` BIGINT      NOT NULL DEFAULT 0 COMMENT '项目ID',
  `name`       VARCHAR(50) NOT NULL DEFAULT '' COMMENT '阶段名称',
  `order_num`  INT         NOT NULL DEFAULT 0 COMMENT '排序号',
  `start_date` DATE        NULL DEFAULT NULL COMMENT '开始日期',
  `end_date`   DATE        NULL DEFAULT NULL COMMENT '截止日期',
  `status`     VARCHAR(16) NOT NULL DEFAULT 'pending' COMMENT 'pending/active/completed',
  `created_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`    TINYINT     NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_project_id` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='阶段表';

-- 6. task 任务表（§8.1.6）
DROP TABLE IF EXISTS `task`;
CREATE TABLE `task` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  `project_id`   BIGINT       NOT NULL DEFAULT 0 COMMENT '项目ID',
  `stage_id`     BIGINT       NOT NULL DEFAULT 0 COMMENT '阶段ID',
  `title`        VARCHAR(100) NOT NULL DEFAULT '' COMMENT '任务标题',
  `description`  TEXT         NULL COMMENT '任务描述',
  `assignee_id`  BIGINT       NOT NULL DEFAULT 0 COMMENT '负责人ID',
  `status`       VARCHAR(16)  NOT NULL DEFAULT 'pending' COMMENT 'pending/in_progress/completed',
  `deadline`     DATETIME     NULL DEFAULT NULL COMMENT '截止时间',
  `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `completed_at` DATETIME     NULL DEFAULT NULL COMMENT '完成时间',
  `updated_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`      TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_project_id` (`project_id`),
  KEY `idx_assignee_id` (`assignee_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务表';

-- 7. file 文件表（§8.1.7）
DROP TABLE IF EXISTS `file`;
CREATE TABLE `file` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '文件ID',
  `project_id`    BIGINT       NOT NULL DEFAULT 0 COMMENT '项目ID',
  `stage_id`      BIGINT       NOT NULL DEFAULT 0 COMMENT '阶段ID',
  `uploader_id`   BIGINT       NOT NULL DEFAULT 0 COMMENT '上传者ID',
  `name`          VARCHAR(255) NOT NULL DEFAULT '' COMMENT '文件名',
  `url`           VARCHAR(500) NOT NULL DEFAULT '' COMMENT '文件URL',
  `size`          BIGINT       NOT NULL DEFAULT 0 COMMENT '文件大小',
  `type`          VARCHAR(50)  NOT NULL DEFAULT '' COMMENT '文件类型',
  `status`        VARCHAR(16)  NOT NULL DEFAULT 'pending' COMMENT 'pending/approved/rejected/archived',
  `reject_reason` TEXT         NULL COMMENT '驳回原因',
  `version`       INT          NOT NULL DEFAULT 1 COMMENT '版本号',
  `is_latest`     TINYINT      NOT NULL DEFAULT 1 COMMENT '是否最新版本',
  `parent_id`     BIGINT       NOT NULL DEFAULT 0 COMMENT '父文件ID（版本关联）',
  `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`       TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_project_id` (`project_id`),
  KEY `idx_stage_id` (`stage_id`),
  KEY `idx_uploader_id` (`uploader_id`),
  KEY `idx_status` (`status`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件表';

-- 8. file_version 文件历史版本表（§5.1 + §5.9）
DROP TABLE IF EXISTS `file_version`;
CREATE TABLE `file_version` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT,
  `file_id`        BIGINT       NOT NULL DEFAULT 0,
  `version_number` VARCHAR(16)  NOT NULL DEFAULT '' COMMENT '版本号字符串，如 1.0/1.1',
  `url`            VARCHAR(500) NOT NULL DEFAULT '',
  `remark`         TEXT         NULL,
  `operator_id`    BIGINT       NOT NULL DEFAULT 0,
  `created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`        TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_file_id` (`file_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件历史版本表';

-- 9. project_share 项目分享表（§8.1.9 ⭐）
DROP TABLE IF EXISTS `project_share`;
CREATE TABLE `project_share` (
  `id`               BIGINT      NOT NULL AUTO_INCREMENT COMMENT '分享ID',
  `project_id`       BIGINT      NOT NULL DEFAULT 0 COMMENT '项目ID',
  `share_token`      VARCHAR(32) NOT NULL DEFAULT '' COMMENT '分享Token',
  `permission_level` VARCHAR(32) NOT NULL DEFAULT 'view_only' COMMENT 'view_only/view_with_preview/view_with_download',
  `view_count`       INT         NOT NULL DEFAULT 0 COMMENT '浏览次数',
  `creator_id`       BIGINT      NOT NULL DEFAULT 0 COMMENT '创建者用户ID，T-02a 增加以支持按用户过滤 myShares',
  `password`         VARCHAR(64) NOT NULL DEFAULT '' COMMENT '访问密码（可选），T-02d 完整版访客视图使用',
  `created_at`       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `expires_at`       DATETIME    NULL DEFAULT NULL COMMENT '过期时间',
  `updated_at`       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`          TINYINT     NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_share_token` (`share_token`),
  KEY `idx_project_id` (`project_id`),
  KEY `idx_creator_id` (`creator_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='项目分享表';

-- 10. operation_log 操作日志表（§8.1.8）
DROP TABLE IF EXISTS `operation_log`;
CREATE TABLE `operation_log` (
  `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `project_id`  BIGINT      NOT NULL DEFAULT 0 COMMENT '项目ID',
  `user_id`     BIGINT      NOT NULL DEFAULT 0 COMMENT '操作者ID',
  `action`      VARCHAR(50) NOT NULL DEFAULT '' COMMENT '操作类型',
  `target_type` VARCHAR(50) NOT NULL DEFAULT '' COMMENT '目标类型',
  `target_id`   BIGINT      NOT NULL DEFAULT 0 COMMENT '目标ID',
  `description` TEXT        NULL COMMENT '操作描述',
  `created_at`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  `updated_at`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`     TINYINT     NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_project_id` (`project_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_action` (`action`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';

-- 11. discussion 研讨表（§5.1 + §5.11）
DROP TABLE IF EXISTS `discussion`;
CREATE TABLE `discussion` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT,
  `project_id`   BIGINT       NOT NULL DEFAULT 0,
  `topic`        VARCHAR(256) NOT NULL DEFAULT '',
  `content`      TEXT         NULL,
  `meeting_time` DATETIME     NULL DEFAULT NULL COMMENT '会议时间',
  `creator_id`   BIGINT       NOT NULL DEFAULT 0,
  `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`      TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_project_id` (`project_id`),
  KEY `idx_creator_id` (`creator_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='研讨表';

-- 12. decision 决策表（§5.1 + §5.11：关联 discussion）
DROP TABLE IF EXISTS `decision`;
CREATE TABLE `decision` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT,
  `project_id`    BIGINT       NOT NULL DEFAULT 0,
  `title`         VARCHAR(256) NOT NULL DEFAULT '',
  `content`       TEXT         NULL,
  `creator_id`    BIGINT       NOT NULL DEFAULT 0,
  `discussion_id` BIGINT       NOT NULL DEFAULT 0 COMMENT '关联研讨',
  `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`       TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_project_id` (`project_id`),
  KEY `idx_discussion_id` (`discussion_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='决策表';

-- 13. achievement 成果表（§5.11：属于 stage）
DROP TABLE IF EXISTS `achievement`;
CREATE TABLE `achievement` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `project_id`  BIGINT       NOT NULL DEFAULT 0,
  `stage_id`    BIGINT       NOT NULL DEFAULT 0,
  `title`       VARCHAR(256) NOT NULL DEFAULT '',
  `description` TEXT         NULL,
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`     TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_project_id` (`project_id`),
  KEY `idx_stage_id` (`stage_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='项目成果表';

-- 14. message 消息表（§5.1 + §5.7）
DROP TABLE IF EXISTS `message`;
CREATE TABLE `message` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`     BIGINT       NOT NULL DEFAULT 0 COMMENT '接收者ID',
  `title`       VARCHAR(256) NOT NULL DEFAULT '',
  `content`     VARCHAR(1024) NOT NULL DEFAULT '',
  `type`        VARCHAR(32)  NOT NULL DEFAULT 'system' COMMENT 'system/task/file/announcement',
  `related_type` VARCHAR(32) NOT NULL DEFAULT '' COMMENT '关联业务类型',
  `related_id`  BIGINT       NOT NULL DEFAULT 0 COMMENT '关联业务ID',
  `is_read`     TINYINT      NOT NULL DEFAULT 0,
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`     TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_is_read` (`is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统消息表';

-- ============================================================
-- 二、扩展表（按 §5 全部类图，凑齐 22 张表）
-- ============================================================

-- 15. login_history 登录历史（§5.2）
DROP TABLE IF EXISTS `login_history`;
CREATE TABLE `login_history` (
  `id`         BIGINT      NOT NULL AUTO_INCREMENT,
  `user_id`    BIGINT      NOT NULL DEFAULT 0,
  `device`     VARCHAR(128) NOT NULL DEFAULT '' COMMENT '设备',
  `ip`         VARCHAR(64) NOT NULL DEFAULT '',
  `login_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`    TINYINT     NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='登录历史表';

-- 16. stage_template 阶段模板（§5.3）
DROP TABLE IF EXISTS `stage_template`;
CREATE TABLE `stage_template` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `name`        VARCHAR(64)  NOT NULL DEFAULT '',
  `description` VARCHAR(512) NOT NULL DEFAULT '',
  `stages_json` TEXT         NULL COMMENT '阶段定义 JSON',
  `created_by`  BIGINT       NOT NULL DEFAULT 0,
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`     TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='阶段模板表';

-- 17. file_audit_log 文件审核日志（§5.4）
DROP TABLE IF EXISTS `file_audit_log`;
CREATE TABLE `file_audit_log` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT,
  `file_id`    BIGINT       NOT NULL DEFAULT 0,
  `auditor_id` BIGINT       NOT NULL DEFAULT 0,
  `action`     VARCHAR(16)  NOT NULL DEFAULT '' COMMENT 'approve/reject',
  `reason`     VARCHAR(1024) NOT NULL DEFAULT '',
  `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`    TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_file_id` (`file_id`),
  KEY `idx_auditor_id` (`auditor_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件审核日志表';

-- 18. task_reminder 任务提醒（§5.5：定时器生成的提醒）
DROP TABLE IF EXISTS `task_reminder`;
CREATE TABLE `task_reminder` (
  `id`            BIGINT      NOT NULL AUTO_INCREMENT,
  `task_id`       BIGINT      NOT NULL DEFAULT 0,
  `reminder_type` VARCHAR(16) NOT NULL DEFAULT 'before_due' COMMENT 'before_due/overdue/assign',
  `remind_at`     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `is_sent`       TINYINT     NOT NULL DEFAULT 0,
  `created_at`    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`       TINYINT     NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_task_id` (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务提醒表';

-- 19. share_visitor 分享访客记录（§5.6）
DROP TABLE IF EXISTS `share_visitor`;
CREATE TABLE `share_visitor` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT,
  `share_id`   BIGINT       NOT NULL DEFAULT 0,
  `ip`         VARCHAR(64)  NOT NULL DEFAULT '',
  `user_agent` VARCHAR(512) NOT NULL DEFAULT '',
  `visited_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`    TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_share_id` (`share_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分享访客记录表';

-- 20. subscription_message 订阅消息授权（§5.7：微信模板消息）
DROP TABLE IF EXISTS `subscription_message`;
CREATE TABLE `subscription_message` (
  `id`         BIGINT      NOT NULL AUTO_INCREMENT,
  `template_id` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '微信模板ID',
  `user_id`    BIGINT      NOT NULL DEFAULT 0,
  `openid`     VARCHAR(64) NOT NULL DEFAULT '',
  `subscribed` TINYINT     NOT NULL DEFAULT 0,
  `expires_at` DATETIME    NULL DEFAULT NULL,
  `created_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`    TINYINT     NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_template_user` (`template_id`, `user_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='微信订阅消息授权表';

-- 21. invite_code 邀请码（§5.8：可独立轮换）
DROP TABLE IF EXISTS `invite_code`;
CREATE TABLE `invite_code` (
  `code`      VARCHAR(8) NOT NULL DEFAULT '' COMMENT '邀请码',
  `group_id`  BIGINT     NOT NULL DEFAULT 0,
  `expires_at` DATETIME  NULL DEFAULT NULL,
  `created_at` DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`   TINYINT    NOT NULL DEFAULT 0,
  PRIMARY KEY (`code`),
  KEY `idx_group_id` (`group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='邀请码表';

-- 22. file_collect 文件收藏（§7 文件管理流程 - 收藏）
DROP TABLE IF EXISTS `file_collect`;
CREATE TABLE `file_collect` (
  `id`         BIGINT   NOT NULL AUTO_INCREMENT,
  `user_id`    BIGINT   NOT NULL DEFAULT 0,
  `file_id`    BIGINT   NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`    TINYINT  NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_file` (`user_id`, `file_id`),
  KEY `idx_file_id` (`file_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件收藏表';

-- 23. user_sms_verify_code 短信验证码（T-25 手机号+验证码登录）
DROP TABLE IF EXISTS `user_sms_verify_code`;
CREATE TABLE `user_sms_verify_code` (
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

SET FOREIGN_KEY_CHECKS = 1;
