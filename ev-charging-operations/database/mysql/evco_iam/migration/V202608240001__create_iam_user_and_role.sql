-- ============================================================
-- 迁移：创建 IAM 管理用户、角色与用户角色绑定表
-- Schema: evco_iam
-- 关联设计：docs/design/W2-IAM个人身份与配置技术设计与契约包.md §3.1、§4.1（IAM 用户状态机）、
--           §8.3（IOT 推送镜像）、§8.5（登录认证）
-- IOT 对齐：t_user（tenant_id 默认系统租户、BCrypt password、user_type、
--           must_change_password、active_username 生成列）、t_role、t_user_role
-- 约定：UTF-8、前向 only、无 down migration、雪花 ID、脚本幂等
-- 平台扩展字段（IOT 推送不覆盖）：locked_until、fail_window_start、fail_count、expire_at、
--           last_login_at、last_login_ip；IOT 管理字段：username、display_name、
--           password_hash、user_type、iot_user_id、status、tenant_id
-- ============================================================

-- ------------------------------------------------------------
-- 1. iam_user 表：管理端用户（平台管理员/租户管理员/运营人员，M3 复用）
--    状态机：active → locked（锁定）；locked → active（解锁）；
--            active/locked → disabled（停用）；disabled → active（恢复，需审计）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `iam_user` (
    `id`                   BIGINT       NOT NULL COMMENT '用户 ID（独立模式平台雪花 ID；IOT 模式沿用 IOT 推送 ID，保持两侧一致）',
    `tenant_id`            BIGINT       NOT NULL DEFAULT 2069700000000000001 COMMENT '所属租户；平台级用户归属系统租户 2069700000000000001（对齐 IOT t_user 默认值）',
    `username`             VARCHAR(64)  NOT NULL COMMENT '登录名；租户范围内唯一（生成列 active_username 实现软删后释放）',
    `display_name`         VARCHAR(64)  NOT NULL COMMENT '后台展示姓名',
    `password_hash`        VARCHAR(100) NOT NULL COMMENT 'BCrypt 密码哈希（含内置盐值）；IOT 协同模式由 IOT 推送，独立模式由平台设置',
    `user_type`            VARCHAR(16)  NOT NULL DEFAULT 'NORMAL' COMMENT '用户类型：PLATFORM_SUPER（平台超级管理员，鉴权短路放行）/ TENANT_ADMIN（租户管理员）/ NORMAL（普通运营人员）；对齐 IOT t_user.user_type',
    `iot_user_id`          BIGINT       NULL     COMMENT 'IOT 平台用户 ID；SSO 免登录定位键，source=IOT_PUSH 时必填，全表唯一',
    `source`               VARCHAR(16)  NOT NULL DEFAULT 'PLATFORM' COMMENT '数据来源：PLATFORM（平台维护）/ IOT_PUSH（IOT 协同模式推送）',
    `status`               VARCHAR(16)  NOT NULL DEFAULT 'active' COMMENT '状态：active（可用）/ locked（锁定）/ disabled（停用）；迁移以 IAM 状态机为准',
    `must_change_password` TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否强制改密（下次登录）；IOT 推送同步标记',
    `locked_until`         DATETIME(3)  NULL     COMMENT '锁定截止时间（登录失败 5 次锁 30 分钟）；平台扩展字段，IOT 推送不覆盖',
    `fail_window_start`    DATETIME(3)  NULL     COMMENT '连续失败窗口起点（用于判定“连续 5 次”）；平台扩展字段',
    `fail_count`           INT          NOT NULL DEFAULT 0 COMMENT '连续登录失败次数；成功登录后清零；平台扩展字段',
    `expire_at`            DATETIME(3)  NULL     COMMENT '账号到期日；登录时校验，到期自动锁定；平台扩展字段',
    `last_login_at`        DATETIME(3)  NULL     COMMENT '最近登录时间（由 auth_session 登录成功后回写）；平台扩展字段',
    `last_login_ip`        VARCHAR(45)  NULL     COMMENT '最近登录 IP（IPv4/IPv6，由 auth_session 回写）；平台扩展字段',
    `version`              INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本；更新时递增，冲突返回 VERSION_CONFLICT',
    `created_at`           DATETIME(3)  NOT NULL COMMENT '创建时间（UTC）',
    `updated_at`           DATETIME(3)  NOT NULL COMMENT '更新时间（UTC）',
    `deleted_at`           DATETIME(3)  NULL     COMMENT '逻辑删除标记；删除后不可见但审计可追溯，禁止物理删除',
    `active_username`      VARCHAR(64)  GENERATED ALWAYS AS (CASE WHEN `deleted_at` IS NULL THEN `username` ELSE NULL END) STORED COMMENT '未删除用户登录名，软删后唯一约束自动释放' NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_tenant_active_username` (`tenant_id`, `active_username`),
    UNIQUE KEY `uk_user_iot_user_id` (`iot_user_id`),
    KEY `idx_user_tenant` (`tenant_id`),
    KEY `idx_user_status` (`status`),
    KEY `idx_user_user_type` (`user_type`),
    KEY `idx_user_source` (`source`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='IAM 管理端用户表';

-- ------------------------------------------------------------
-- 2. iam_role 表：角色（对齐 IOT t_role）
--    平台级角色归属系统租户 2069700000000000001；租户级角色归属对应租户
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `iam_role` (
    `id`          BIGINT       NOT NULL COMMENT '角色 ID（独立模式平台雪花 ID；IOT 模式沿用 IOT 推送 ID）',
    `tenant_id`   BIGINT       NOT NULL DEFAULT 2069700000000000001 COMMENT '所属租户；平台级角色使用系统租户 2069700000000000001（对齐 IOT t_role 默认值）',
    `code`        VARCHAR(64)  NOT NULL COMMENT '角色编码；租户内唯一（生成列 active_code 实现软删后释放）',
    `name`        VARCHAR(64)  NOT NULL COMMENT '角色名称',
    `description` VARCHAR(255) NULL     COMMENT '角色描述',
    `source`      VARCHAR(16)  NOT NULL DEFAULT 'PLATFORM' COMMENT '数据来源：PLATFORM（平台维护）/ IOT_PUSH（IOT 协同模式推送）',
    `created_at`  DATETIME(3)  NOT NULL COMMENT '创建时间（UTC）',
    `updated_at`  DATETIME(3)  NOT NULL COMMENT '更新时间（UTC）',
    `deleted_at`  DATETIME(3)  NULL     COMMENT '逻辑删除标记；删除后编码可复用',
    `active_code` VARCHAR(64)  GENERATED ALWAYS AS (CASE WHEN `deleted_at` IS NULL THEN `code` ELSE NULL END) STORED COMMENT '未删除角色编码，软删后唯一约束自动释放' NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_tenant_active_code` (`tenant_id`, `active_code`),
    KEY `idx_role_tenant` (`tenant_id`),
    KEY `idx_role_source` (`source`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='IAM 角色表';

-- ------------------------------------------------------------
-- 3. iam_user_role 表：用户-角色绑定（多对多，对齐 IOT t_user_role）
--    绑定变更（新增/替换/解绑）由 iam_audit_log 记录审计
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `iam_user_role` (
    `id`     BIGINT      NOT NULL COMMENT '绑定 ID（雪花 ID）',
    `user_id`  BIGINT    NOT NULL COMMENT '用户 ID（iam_user）',
    `role_id`  BIGINT    NOT NULL COMMENT '角色 ID（iam_role）',
    `source`   VARCHAR(16) NOT NULL DEFAULT 'PLATFORM' COMMENT '数据来源：PLATFORM（平台维护）/ IOT_PUSH（IOT 协同模式推送）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_role` (`user_id`, `role_id`),
    KEY `idx_user_role_role` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='IAM 用户-角色绑定表';

-- ============================================================
-- 4. 校验（执行后人工/自动化核对）
-- ============================================================
-- 预期：SHOW TABLES LIKE 'iam_%'; => iam_user、iam_role、iam_user_role（V005 的 8 张表已存在）
-- 预期：SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
--        WHERE TABLE_SCHEMA = DATABASE() AND CONSTRAINT_TYPE = 'UNIQUE' AND TABLE_NAME = 'iam_user'; => 2
-- 说明：系统租户 ID 2069700000000000001 与 IOT linkos 默认值一致，保证两侧 ID 语义一致；
--       平台扩展字段（locked_until 等）在 IOT 推送 upsert 时不被覆盖（见设计包 §8.3）
