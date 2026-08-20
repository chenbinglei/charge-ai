-- ============================================================
-- 迁移：初始化平台超级管理员引导账号（W2 缺口补齐）
-- Schema: evco_iam
-- 关联设计：docs/design/W2-IAM个人身份与配置技术设计与契约包.md §4.1（用户状态机）、§8.5（登录认证）
-- 背景：V202608240001 仅建表未播种用户，运行库无任何引导账号，
--       登录链路（POST /api/v1/auth/login）与 W2 Step 5 前端开发均无账号可用。
-- 账号：admin / EvcoAdmin@2026（BCrypt cost=10，与平台 PasswordEncoder 一致）。
--       仅为本地/开发引导口令（口径同 compose 默认凭据）；生产首次部署后
--       必须立即登录改密，真实凭据仅经 P1 授权页面与受控系统维护。
-- 超管机制：user_type='PLATFORM_SUPER' 鉴权短路（PermissionServiceImpl），
--           直接返回全部 ACTIVE 权限（159 条），不依赖角色绑定；roles 为空。
-- 约定：UTF-8、前向 only、无 down migration、雪花 ID、脚本幂等；
--       ID 使用保留段 1000000000003000001，不与运行时雪花 ID 冲突。
-- ============================================================

SET @now := NOW();

INSERT INTO `iam_user`
    (`id`, `tenant_id`, `username`, `display_name`, `password_hash`, `user_type`,
     `source`, `status`, `must_change_password`, `version`, `created_at`, `updated_at`)
VALUES
    (1000000000003000001, 2069700000000000001, 'admin', '平台超级管理员',
     '$2a$10$KLFhK92ztqq7JmEYC8ll..aAoWfNJe62rClmzd.kzzmCicT7FEEF6', 'PLATFORM_SUPER',
     'PLATFORM', 'active', 0, 0, @now, @now)
ON DUPLICATE KEY UPDATE `id` = `id`;

-- ============================================================
-- 校验（执行后人工/自动化核对）
-- ============================================================
-- 预期：SELECT COUNT(*) FROM iam_user;                                              => 1
-- 预期：SELECT user_type, status FROM iam_user WHERE username = 'admin';            => PLATFORM_SUPER / active
-- 登录验证（服务 8081 端口）：GET /api/v1/auth/captcha → POST /api/v1/auth/login
--   {username: "admin", password: "EvcoAdmin@2026", captchaId, captchaCode}
--   → GET /api/v1/auth/profile 应返回 159 条权限码与 P1 全量菜单树（73 叶子）。
