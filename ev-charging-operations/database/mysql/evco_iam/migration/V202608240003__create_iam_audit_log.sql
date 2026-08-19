-- ============================================================
-- 迁移：创建 IAM 操作审计日志表
-- Schema: evco_iam
-- 关联设计：docs/design/W2-IAM个人身份与配置技术设计与契约包.md §3.1
-- 记录范围：用户 CRUD、角色绑定替换、状态迁移、登录/登出/令牌撤销等敏感操作
-- 约定：UTF-8、前向 only、无 down migration、雪花 ID、脚本幂等；
--       追加只写（append-only），不更新不删除；90 天后冷存储归档
-- ============================================================

CREATE TABLE IF NOT EXISTS `iam_audit_log` (
    `id`          BIGINT       NOT NULL COMMENT '日志 ID（雪花 ID）',
    `tenant_id`   BIGINT       NULL     COMMENT '租户 ID；平台级操作为系统租户 2069700000000000001',
    `actor_id`    BIGINT       NULL     COMMENT '操作者用户 ID（iam_user）；系统内部操作为 NULL',
    `action`      VARCHAR(32)  NOT NULL COMMENT '操作类型：create/update/delete/status_change/role_replace/login/logout/token_revoke',
    `target_type` VARCHAR(32)  NOT NULL COMMENT '目标对象类型：iam_user/iam_role/auth_session/refresh_token 等',
    `target_id`   BIGINT       NULL     COMMENT '目标对象 ID；登录/登出等无明确目标时为 NULL',
    `trace_id`    VARCHAR(64)  NULL     COMMENT '请求链路追踪 ID；与网关/服务日志关联',
    `summary`     VARCHAR(512) NOT NULL COMMENT '脱敏操作摘要（不含密码哈希、令牌原文等敏感材料）',
    `ip`          VARCHAR(45)  NULL     COMMENT '操作来源 IP（IPv4/IPv6）；登录/SSO 审计必填',
    `created_at`  DATETIME(3)  NOT NULL COMMENT '操作时间（UTC）',
    PRIMARY KEY (`id`),
    KEY `idx_audit_created_at` (`created_at`),
    KEY `idx_audit_actor` (`actor_id`, `created_at`),
    KEY `idx_audit_target` (`target_type`, `target_id`),
    KEY `idx_audit_tenant_time` (`tenant_id`, `created_at`),
    KEY `idx_audit_trace_id` (`trace_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='IAM 操作审计日志表（追加只写）';

-- ============================================================
-- 校验（执行后人工/自动化核对）
-- ============================================================
-- 预期：SHOW TABLES LIKE 'iam_audit_log'; => iam_audit_log
-- 说明：本表无 updated_at/deleted_at——追加只写语义；冷归档策略见数据治理矩阵
