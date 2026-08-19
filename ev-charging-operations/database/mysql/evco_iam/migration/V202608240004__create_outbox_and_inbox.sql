-- ============================================================
-- 迁移：创建 evco_iam 的事件外发箱与收件箱表（Outbox/Inbox 模式）
-- Schema: evco_iam
-- 关联设计：docs/design/W1-工程跑道技术设计与契约包.md §3（Outbox/Inbox 规则）、
--           docs/design/W2-IAM个人身份与配置技术设计与契约包.md §3.1、§3.4（W2 事件清单）
-- 规则：业务事实与 Outbox 同一事务提交；消费者将 Inbox、自己的事实和处理结果
--       同一事务提交后才提交 Kafka offset
-- 约定：UTF-8、前向 only、无 down migration、雪花 ID、脚本幂等；
--       W2 只记录事件不发布（Topic 分区数/保留期 W4 冻结后才创建）
-- ============================================================

-- ------------------------------------------------------------
-- 1. outbox_event 表：跨服务事件外发箱
--    事件类型（W2）：iam.user.created.v1、iam.user.status_changed.v1、
--    iam.user.deleted.v1、iam.role.replaced.v1、auth.session.revoked.v1
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `outbox_event` (
    `id`              BIGINT       NOT NULL COMMENT '事件 ID（雪花 ID）',
    `event_id`        VARCHAR(64)  NOT NULL COMMENT '事件业务唯一标识（UUID）；消费者幂等去重键',
    `event_type`      VARCHAR(64)  NOT NULL COMMENT '事件类型与大版本，如 iam.user.created.v1',
    `payload_summary` VARCHAR(512) NOT NULL COMMENT '载荷脱敏摘要（不含 L3 敏感原文；完整载荷由发布器从业务事实重建）',
    `status`          VARCHAR(32)  NOT NULL DEFAULT 'PENDING' COMMENT '发布状态：PENDING（待发布）/ PUBLISHED（已发布）/ RETRYABLE_FAILURE（可重试失败）/ MANUAL_REVIEW（待人工处置）',
    `retry_count`     INT          NOT NULL DEFAULT 0 COMMENT '已重试次数',
    `next_retry_at`   DATETIME(3)  NULL     COMMENT '下次重试时间；指数退避',
    `published_at`    DATETIME(3)  NULL     COMMENT '发布成功时间（UTC）',
    `trace_id`        VARCHAR(64)  NULL     COMMENT '产生事件的操作链路追踪 ID',
    `created_at`      DATETIME(3)  NOT NULL COMMENT '创建时间（UTC）',
    `updated_at`      DATETIME(3)  NOT NULL COMMENT '更新时间（UTC）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_outbox_event_id` (`event_id`),
    KEY `idx_outbox_status_retry` (`status`, `next_retry_at`),
    KEY `idx_outbox_event_type_time` (`event_type`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='事件外发箱（业务事实同事务写入）';

-- ------------------------------------------------------------
-- 2. inbox_event 表：消费侧收件箱（本 Schema 消费的外部事件）
--    consumer_service + event_id 唯一保证幂等；处理结果与自身事实同事务落库
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `inbox_event` (
    `id`               BIGINT       NOT NULL COMMENT '收件 ID（雪花 ID）',
    `consumer_service` VARCHAR(64)  NOT NULL COMMENT '消费者服务名，如 platform-service；同一事件可被多服务分别消费',
    `event_id`         VARCHAR(64)  NOT NULL COMMENT '事件业务唯一标识；与 outbox_event.event_id 对应',
    `event_type`       VARCHAR(64)  NOT NULL COMMENT '事件类型与大版本',
    `result`           VARCHAR(32)  NOT NULL COMMENT '处理结果：APPLIED（已应用）/ DUPLICATE（重复投递）/ RETRYABLE_FAILURE（可重试失败）/ DLQ（死信）',
    `failure_reason`   VARCHAR(512) NULL     COMMENT '失败原因摘要；人工处置引用',
    `trace_id`         VARCHAR(64)  NULL     COMMENT '事件链路追踪 ID',
    `created_at`       DATETIME(3)  NOT NULL COMMENT '首次收件时间（UTC）',
    `updated_at`       DATETIME(3)  NOT NULL COMMENT '更新时间（UTC）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_inbox_consumer_event` (`consumer_service`, `event_id`),
    KEY `idx_inbox_result` (`result`),
    KEY `idx_inbox_event_type` (`event_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='事件收件箱（消费幂等与结果留痕）';

-- ============================================================
-- 3. 校验（执行后人工/自动化核对）
-- ============================================================
-- 预期：SHOW TABLES LIKE '%_event'; => outbox_event、inbox_event
-- 预期：SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
--        WHERE TABLE_SCHEMA = DATABASE() AND CONSTRAINT_TYPE = 'UNIQUE' AND TABLE_NAME = 'inbox_event'; => 1
-- 说明：W2 范围内 platform-service 尚无 Kafka 发布/消费运行时（Topic W4 冻结后创建），
--       本表由 IAM 业务写入路径同事务填充，发布器与消费者在 W4 接入
