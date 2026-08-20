-- ============================================================
-- 迁移：创建认证会话与刷新令牌表
-- Schema: evco_iam
-- 关联设计：docs/design/W2-IAM个人身份与配置技术设计与契约包.md §3.1、§4.3（认证服务状态机）、
--           §8.5（登录认证）、§8.6（SSO 免登录）
-- 状态机：auth_session active → expired（TTL 过期）/ revoked（登出/撤销）；
--          refresh_token active → used（一次性使用）/ revoked
-- 约定：UTF-8、前向 only、无 down migration、雪花 ID、脚本幂等；
--       会话/令牌为短生命周期运行数据，无逻辑删除，生命周期由 status 表达
-- ============================================================

-- ------------------------------------------------------------
-- 1. auth_session 表：登录会话（密码登录与 SSO 免登录共用）
--    auth_type=SSO 标记 IOT 平台免登录进入；多端会话数量上限由
--    platform_setting 配置（登录时按 user_id 统计 active 会话数控制）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `auth_session` (
    `id`          BIGINT       NOT NULL COMMENT '会话 ID（雪花 ID）',
    `user_id`     BIGINT       NOT NULL COMMENT '用户 ID（iam_user）',
    `auth_type`   VARCHAR(16)  NOT NULL DEFAULT 'PASSWORD' COMMENT '认证方式：PASSWORD（账号密码登录）/ SSO（IOT 协同模式免登录换会话）',
    `status`      VARCHAR(16)  NOT NULL DEFAULT 'active' COMMENT '状态：active（有效）/ expired（TTL 过期）/ revoked（登出或撤销）',
    `ip`          VARCHAR(15)  NULL     COMMENT '登录来源 IP（IPv4；非 IPv4 来源存 NULL）；审计用',
    `user_agent`  VARCHAR(255) NULL     COMMENT '登录端 User-Agent 摘要；多端会话管理展示用',
    `expires_at`  DATETIME     NOT NULL COMMENT '会话过期时间（TTL：access_token 有效期对齐，15-30 分钟滑动续期）',
    `created_at`  DATETIME     NOT NULL COMMENT '创建时间（北京时间）',
    `updated_at`  DATETIME     NOT NULL COMMENT '更新时间（北京时间）',
    PRIMARY KEY (`id`),
    KEY `idx_session_user_status` (`user_id`, `status`),
    KEY `idx_session_status` (`status`),
    KEY `idx_session_expires_at` (`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='认证会话表';

-- ------------------------------------------------------------
-- 2. refresh_token 表：刷新令牌（一次性使用）
--    token_hash 存储 SHA-256 摘要，原文令牌不落库；
--    会话撤销时关联刷新令牌一并失效
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `refresh_token` (
    `id`          BIGINT       NOT NULL COMMENT '刷新令牌 ID（雪花 ID）',
    `session_id`  BIGINT       NOT NULL COMMENT '所属会话 ID（auth_session）；会话撤销时本令牌级联失效',
    `user_id`     BIGINT       NOT NULL COMMENT '用户 ID（冗余 iam_user.id，按用户撤销时免关联查询）',
    `token_hash`  VARCHAR(128) NOT NULL COMMENT '刷新令牌 SHA-256 摘要；原文不落库，防拖库重放',
    `status`      VARCHAR(16)  NOT NULL DEFAULT 'active' COMMENT '状态：active（可用）/ used（已使用，一次性）/ revoked（已撤销）',
    `expires_at`  DATETIME     NOT NULL COMMENT '过期时间（长 TTL：7-30 天）',
    `created_at`  DATETIME     NOT NULL COMMENT '创建时间（北京时间）',
    `updated_at`  DATETIME     NOT NULL COMMENT '更新时间（北京时间）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_refresh_token_hash` (`token_hash`),
    KEY `idx_refresh_token_session` (`session_id`),
    KEY `idx_refresh_token_user` (`user_id`),
    KEY `idx_refresh_token_expires_at` (`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='刷新令牌表（一次性使用）';

-- ============================================================
-- 3. 校验（执行后人工/自动化核对）
-- ============================================================
-- 预期：SHOW TABLES LIKE 'auth_%'; => auth_session、refresh_token
-- 预期：SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
--        WHERE TABLE_SCHEMA = DATABASE() AND CONSTRAINT_TYPE = 'UNIQUE' AND TABLE_NAME = 'refresh_token'; => 1
-- 说明：access_token 为无状态 JWT（Redis 缓存权限列表，TTL 与 expires_at 一致，见设计包 §4.3），
--       不建表；权限列表缓存 Redis 键规则见平台技术命名规范 evco:{env}:{domain}:{entity}:{id}
