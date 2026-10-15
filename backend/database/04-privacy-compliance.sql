-- P3-5 隐私合规：user 表加注销相关字段
-- 注意：BaseEntity 已经维护了 deleted 列，所以这里只补 canceled_at/purge_at/cancel_reason
ALTER TABLE `user`
  ADD COLUMN `canceled_at`   DATETIME     NULL                 COMMENT '注销时间' AFTER `deleted`,
  ADD COLUMN `purge_at`      DATETIME     NULL                 COMMENT '预计物理删除时间' AFTER `canceled_at`,
  ADD COLUMN `cancel_reason` VARCHAR(255) NULL                 COMMENT '注销原因' AFTER `purge_at`;

CREATE INDEX `idx_user_purge` ON `user` (`deleted`, `purge_at`);
