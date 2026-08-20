-- ============================================================
-- 迁移：创建与 IOT linkos 对齐的两档权限模型表，并初始化 W2 权限
-- Schema: evco_iam
-- 关联设计：docs/design/W2-IAM个人身份与配置技术设计与契约包.md §7（权限模型）、§8（双模式设计）
-- 模型：资源 + 读/写两档（code = resource:action），对齐 IOT linkos
--       t_permission / t_permission_policy / t_permission_policy_item /
--       t_role_permission / t_role_policy / t_org_data_scope /
--       t_tenant_data_scope / t_tenant_grant
-- 约定：UTF-8、前向 only、无 down migration、雪花 ID、脚本幂等
-- 初始化：W2 阶段 10 个资源共 18 条权限 + 预置策略 POLICY_SYSTEM_CONFIG（16 条明细）；
--         W3-W11 由各自交付周迁移按周追加；IOT 协同模式下同编码权限由 IOT 推送覆盖维护（source=IOT_PUSH）
-- ============================================================

-- ------------------------------------------------------------
-- 1. iam_permission 表：权限主数据（两档 read/write）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `iam_permission` (
    `id`          BIGINT       NOT NULL COMMENT '权限 ID（独立模式为平台雪花 ID；IOT 模式沿用 IOT 推送 ID；种子数据使用保留段 1000000000000000001+）',
    `code`        VARCHAR(128) NOT NULL COMMENT '权限编码，格式 resource:action，如 iam_user:write（与 IOT t_permission.code 共用编码体系）',
    `name`        VARCHAR(64)  NOT NULL COMMENT '权限中文名称',
    `resource`    VARCHAR(64)  NOT NULL COMMENT '资源标识，如 iam_user、station',
    `action`      VARCHAR(16)  NOT NULL COMMENT '动作：read（只读）/ write（读写，含新增/编辑/删除/导出/审批等全部写操作）',
    `scope`       VARCHAR(16)  NOT NULL COMMENT '权限层级：PLATFORM（平台级，P1/M3）/ TENANT（租户级，P1/M3）/ ENTERPRISE（企业级，P2/M2/M4）',
    `menu_path`   VARCHAR(128) NULL     COMMENT '对应三级菜单路由；平台扩展字段，IOT 推送时由平台按编码补充映射',
    `description` VARCHAR(256) NULL     COMMENT '权限描述',
    `is_builtin`  TINYINT      NOT NULL DEFAULT 1 COMMENT '是否内置权限；内置权限不可删除',
    `status`      VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE' COMMENT '枚举 ACTIVE/INACTIVE；关闭态功能（银联/银盛条件渠道、V2G 真实放电、提现等）初始化为 INACTIVE',
    `phase`       VARCHAR(8)   NULL     COMMENT '首次交付工作周，如 W2/W3/W7',
    `source`      VARCHAR(16)  NOT NULL DEFAULT 'PLATFORM' COMMENT '数据来源：PLATFORM（平台预置）/ IOT_PUSH（IOT 协同模式推送）',
    `created_at`  DATETIME  NOT NULL COMMENT '创建时间（北京时间）',
    `updated_at`  DATETIME  NOT NULL COMMENT '更新时间（北京时间）',
    `deleted_at`  DATETIME  NULL     COMMENT '逻辑删除标记；内置权限不可删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_permission_code` (`code`),
    KEY `idx_permission_resource` (`resource`),
    KEY `idx_permission_scope` (`scope`),
    KEY `idx_permission_status` (`status`),
    KEY `idx_permission_menu_path` (`menu_path`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限主数据表（资源 + 读/写两档）';

-- ------------------------------------------------------------
-- 2. iam_permission_policy 表：权限策略（权限集合，用于批量授权）
--    对齐 IOT t_permission_policy；平台级预置策略 tenant_id=NULL
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `iam_permission_policy` (
    `id`            BIGINT       NOT NULL COMMENT '策略 ID（雪花 ID；种子数据使用保留段 1000000000001000001+）',
    `tenant_id`     BIGINT       NULL     COMMENT '归属租户；NULL=平台级预置策略，非空=租户级自定义策略',
    `tenant_id_key` BIGINT       GENERATED ALWAYS AS (IFNULL(`tenant_id`, 0)) STORED COMMENT '租户键；平台级策略归一为 0，用于唯一约束',
    `code`          VARCHAR(64)  NOT NULL COMMENT '策略编码，租户内唯一，软删后可复用',
    `name`          VARCHAR(128) NOT NULL COMMENT '策略名称',
    `description`   VARCHAR(255) NULL     COMMENT '策略描述',
    `builtin`       TINYINT      NOT NULL DEFAULT 0 COMMENT '预置核心策略不可删',
    `status`        VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE' COMMENT '枚举 ACTIVE/DISABLED',
    `source`        VARCHAR(16)  NOT NULL DEFAULT 'PLATFORM' COMMENT '数据来源：PLATFORM/IOT_PUSH',
    `created_at`    DATETIME  NOT NULL COMMENT '创建时间（北京时间）',
    `updated_at`    DATETIME  NOT NULL COMMENT '更新时间（北京时间）',
    `deleted_at`    DATETIME  NULL     COMMENT '逻辑删除标记；builtin=1 的预置策略不可删除',
    `active_code`   VARCHAR(64)  GENERATED ALWAYS AS (CASE WHEN `deleted_at` IS NULL THEN `code` ELSE NULL END) STORED COMMENT '未删除策略编码，软删后唯一约束自动释放' NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_policy_tenant_active_code` (`tenant_id_key`, `active_code`),
    KEY `idx_policy_tenant` (`tenant_id`),
    KEY `idx_policy_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限策略表（权限集合）';

-- ------------------------------------------------------------
-- 3. iam_permission_policy_item 表：策略-权限明细（纯关联表）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `iam_permission_policy_item` (
    `id`            BIGINT NOT NULL COMMENT '明细 ID（雪花 ID；种子数据使用保留段 1000000000002000001+）',
    `policy_id`     BIGINT NOT NULL COMMENT '策略 ID',
    `permission_id` BIGINT NOT NULL COMMENT '权限 ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_policy_item` (`policy_id`, `permission_id`),
    KEY `idx_ppi_permission` (`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='策略-权限明细表';

-- ------------------------------------------------------------
-- 4. iam_role_permission 表：角色-权限直接绑定（纯关联表）
--    原设计的 role_type/data_scope/status 字段废弃：
--    数据范围移至站点级授权表，禁用通过解绑实现
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `iam_role_permission` (
    `id`            BIGINT NOT NULL COMMENT '绑定 ID（雪花 ID）',
    `role_id`       BIGINT NOT NULL COMMENT '角色 ID（iam_role）',
    `permission_id` BIGINT NOT NULL COMMENT '权限 ID（iam_permission）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_permission` (`role_id`, `permission_id`),
    KEY `idx_rp_permission` (`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色-权限直接绑定表';

-- ------------------------------------------------------------
-- 5. iam_role_policy 表：角色-策略绑定（纯关联表）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `iam_role_policy` (
    `id`        BIGINT NOT NULL COMMENT '绑定 ID（雪花 ID）',
    `role_id`   BIGINT NOT NULL COMMENT '角色 ID（iam_role）',
    `policy_id` BIGINT NOT NULL COMMENT '策略 ID（iam_permission_policy）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_policy` (`role_id`, `policy_id`),
    KEY `idx_rp_policy` (`policy_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色-策略绑定表';

-- ------------------------------------------------------------
-- 6. iam_org_data_scope 表：组织→站点数据授权（对齐 IOT t_org_data_scope）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `iam_org_data_scope` (
    `id`              BIGINT      NOT NULL COMMENT '授权 ID（雪花 ID）',
    `org_id`          BIGINT      NOT NULL COMMENT '组织 ID（iam_org）',
    `station_id`      BIGINT      NOT NULL COMMENT '站点 ID',
    `access`          VARCHAR(8)  NOT NULL COMMENT '访问级别：READ/WRITE',
    `include_sub_org` TINYINT     NOT NULL DEFAULT 0 COMMENT '是否包含子组织',
    `source`          VARCHAR(16) NOT NULL DEFAULT 'PLATFORM' COMMENT '数据来源：PLATFORM/IOT_PUSH',
    `created_at`      DATETIME NOT NULL COMMENT '创建时间（北京时间）',
    `updated_at`      DATETIME NOT NULL COMMENT '更新时间（北京时间）',
    `deleted_at`      DATETIME NULL     COMMENT '逻辑删除标记',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_org_data_scope` (`org_id`, `station_id`),
    KEY `idx_org_data_scope_station` (`station_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='组织-站点数据授权表';

-- ------------------------------------------------------------
-- 7. iam_tenant_data_scope 表：租户→站点数据权限上限（对齐 IOT t_tenant_data_scope）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `iam_tenant_data_scope` (
    `id`         BIGINT      NOT NULL COMMENT '授权 ID（雪花 ID）',
    `tenant_id`  BIGINT      NOT NULL COMMENT '租户 ID',
    `station_id` BIGINT      NOT NULL COMMENT '站点 ID',
    `access`     VARCHAR(8)  NOT NULL COMMENT '访问级别：READ/WRITE',
    `source`     VARCHAR(16) NOT NULL DEFAULT 'PLATFORM' COMMENT '数据来源：PLATFORM/IOT_PUSH',
    `created_at` DATETIME NOT NULL COMMENT '创建时间（北京时间）',
    `updated_at` DATETIME NOT NULL COMMENT '更新时间（北京时间）',
    `deleted_at` DATETIME NULL     COMMENT '逻辑删除标记',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_data_scope` (`tenant_id`, `station_id`, `access`),
    KEY `idx_tenant_data_scope_station` (`station_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='租户-站点数据权限上限表';

-- ------------------------------------------------------------
-- 8. iam_tenant_grant 表：租户功能授权上限（对齐 IOT t_tenant_grant）
--    对应「租户权限分配」菜单；租户管理员可授予的权限不得超出此上限
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `iam_tenant_grant` (
    `id`            BIGINT      NOT NULL COMMENT '授权 ID（雪花 ID）',
    `tenant_id`     BIGINT      NOT NULL COMMENT '租户 ID',
    `permission_id` BIGINT      NOT NULL COMMENT '权限 ID（iam_permission）',
    `source`        VARCHAR(16) NOT NULL DEFAULT 'PLATFORM' COMMENT '数据来源：PLATFORM/IOT_PUSH',
    `created_at`    DATETIME NOT NULL COMMENT '创建时间（北京时间）',
    `updated_at`    DATETIME NOT NULL COMMENT '更新时间（北京时间）',
    `deleted_at`    DATETIME NULL     COMMENT '逻辑删除标记',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_grant` (`tenant_id`, `permission_id`),
    KEY `idx_tenant_grant_permission` (`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='租户功能授权上限表';

-- ============================================================
-- 9. 初始化 W2 权限数据：10 个资源共 18 条（两档）
--    ID 使用保留段 1000000000000000001+，不与运行时雪花 ID 冲突
--    W3-W11 权限由各自交付周迁移追加；本脚本幂等可重复执行
-- ============================================================
SET @now := NOW();

INSERT INTO `iam_permission`
    (`id`, `code`, `name`, `resource`, `action`, `scope`, `menu_path`, `description`, `is_builtin`, `status`, `phase`, `source`, `created_at`, `updated_at`)
VALUES
    -- iam_user 平台管理员（/system-config/permission-management/platform-admin）
    (1000000000000000001, 'iam_user:read',              '平台管理员-查看',     'iam_user',           'read',  'PLATFORM', '/system-config/permission-management/platform-admin',  '查看平台管理员列表与详情',                       1, 'ACTIVE', 'W2', 'PLATFORM', @now, @now),
    (1000000000000000002, 'iam_user:write',             '平台管理员-管理',     'iam_user',           'write', 'PLATFORM', '/system-config/permission-management/platform-admin',  '平台管理员新增/编辑/删除/状态/角色绑定等写操作',  1, 'ACTIVE', 'W2', 'PLATFORM', @now, @now),
    -- iam_role 平台角色（平台管理员页 Tab 与角色下拉，纯查看；角色写操作归 IOT/平台按双模式维护）
    (1000000000000000018, 'iam_role:read',              '平台角色-查看',       'iam_role',           'read',  'PLATFORM', '/system-config/permission-management/platform-admin',  '查看角色列表与角色下拉选项（roles/options 鉴权）', 1, 'ACTIVE', 'W2', 'PLATFORM', @now, @now),
    -- tenant_permission 租户权限分配（/system-config/permission-management/tenant-permission）
    (1000000000000000003, 'tenant_permission:read',     '租户权限分配-查看',   'tenant_permission',  'read',  'PLATFORM', '/system-config/permission-management/tenant-permission', '查看租户可授权范围',                           1, 'ACTIVE', 'W2', 'PLATFORM', @now, @now),
    (1000000000000000004, 'tenant_permission:write',    '租户权限分配-管理',   'tenant_permission',  'write', 'PLATFORM', '/system-config/permission-management/tenant-permission', '设定/调整租户功能授权上限',                     1, 'ACTIVE', 'W2', 'PLATFORM', @now, @now),
    -- mkt_permission 营销权限（/system-config/permission-management/marketing-permission）
    (1000000000000000005, 'mkt_permission:read',        '营销权限-查看',       'mkt_permission',     'read',  'PLATFORM', '/system-config/permission-management/marketing-permission', '查看营销权限配置',                            1, 'ACTIVE', 'W2', 'PLATFORM', @now, @now),
    (1000000000000000006, 'mkt_permission:write',       '营销权限-管理',       'mkt_permission',     'write', 'PLATFORM', '/system-config/permission-management/marketing-permission', '营销权限配置全部写操作',                       1, 'ACTIVE', 'W2', 'PLATFORM', @now, @now),
    -- auth_audit 授权审计（纯查看，/system-config/permission-management/auth-audit）
    (1000000000000000007, 'auth_audit:read',            '授权审计-查看',       'auth_audit',         'read',  'PLATFORM', '/system-config/permission-management/auth-audit',       '查看授权审计记录',                               1, 'ACTIVE', 'W2', 'PLATFORM', @now, @now),
    -- mini_program 小程序管理（/system-config/app-content/mini-program）
    (1000000000000000008, 'mini_program:read',          '小程序管理-查看',     'mini_program',       'read',  'PLATFORM', '/system-config/app-content/mini-program',              '查看小程序应用档案',                             1, 'ACTIVE', 'W2', 'PLATFORM', @now, @now),
    (1000000000000000009, 'mini_program:write',         '小程序管理-管理',     'mini_program',       'write', 'PLATFORM', '/system-config/app-content/mini-program',              '小程序应用档案全部写操作',                       1, 'ACTIVE', 'W2', 'PLATFORM', @now, @now),
    -- payment_channel 支付渠道与路由（/system-config/trade-config/payment-channel）
    (1000000000000000010, 'payment_channel:read',       '支付渠道与路由-查看', 'payment_channel',    'read',  'PLATFORM', '/system-config/trade-config/payment-channel',          '查看支付渠道与路由配置',                         1, 'ACTIVE', 'W2', 'PLATFORM', @now, @now),
    (1000000000000000011, 'payment_channel:write',      '支付渠道与路由-管理', 'payment_channel',    'write', 'PLATFORM', '/system-config/trade-config/payment-channel',          '支付渠道与路由配置全部写操作',                   1, 'ACTIVE', 'W2', 'PLATFORM', @now, @now),
    -- settlement_channel 结算渠道配置（/system-config/trade-config/settlement-channel）
    (1000000000000000012, 'settlement_channel:read',    '结算渠道配置-查看',   'settlement_channel', 'read',  'PLATFORM', '/system-config/trade-config/settlement-channel',       '查看结算渠道配置',                               1, 'ACTIVE', 'W2', 'PLATFORM', @now, @now),
    (1000000000000000013, 'settlement_channel:write',   '结算渠道配置-管理',   'settlement_channel', 'write', 'PLATFORM', '/system-config/trade-config/settlement-channel',       '结算渠道配置全部写操作',                         1, 'ACTIVE', 'W2', 'PLATFORM', @now, @now),
    -- platform_setting 基础设置（/system-config/base-setting/platform-setting）
    (1000000000000000014, 'platform_setting:read',      '基础设置-查看',       'platform_setting',   'read',  'PLATFORM', '/system-config/base-setting/platform-setting',         '查看平台基础参数',                               1, 'ACTIVE', 'W2', 'PLATFORM', @now, @now),
    (1000000000000000015, 'platform_setting:write',     '基础设置-管理',       'platform_setting',   'write', 'PLATFORM', '/system-config/base-setting/platform-setting',         '平台基础参数全部写操作',                         1, 'ACTIVE', 'W2', 'PLATFORM', @now, @now),
    -- personal_user 个人用户（/customer-equity/personal-customer/personal-user）
    (1000000000000000016, 'personal_user:read',         '个人用户-查看',       'personal_user',      'read',  'PLATFORM', '/customer-equity/personal-customer/personal-user',      '查看个人用户列表/详情/渠道身份冲突',             1, 'ACTIVE', 'W2', 'PLATFORM', @now, @now),
    (1000000000000000017, 'personal_user:write',        '个人用户-管理',       'personal_user',      'write', 'PLATFORM', '/customer-equity/personal-customer/personal-user',      '个人用户资料编辑与冲突人工处置等写操作',         1, 'ACTIVE', 'W2', 'PLATFORM', @now, @now)
ON DUPLICATE KEY UPDATE `id` = `id`;

-- ============================================================
-- 10. 初始化预置策略 POLICY_SYSTEM_CONFIG（平台级 builtin，tenant_id=NULL）
--     包含系统配置域 9 个资源 16 条权限（iam_user、iam_role、tenant_permission、
--     mkt_permission、auth_audit、mini_program、payment_channel、
--     settlement_channel、platform_setting；不含 personal_user）
--     其余预置策略（POLICY_ASSET_FULL 等）依赖 W3+ 权限，由对应交付周迁移初始化
-- ============================================================
INSERT INTO `iam_permission_policy`
    (`id`, `tenant_id`, `code`, `name`, `description`, `builtin`, `status`, `source`, `created_at`, `updated_at`)
VALUES
    (1000000000001000001, NULL, 'POLICY_SYSTEM_CONFIG', '系统配置管理',
     '平台系统与配置域全量权限：平台管理员、租户权限分配、营销权限、授权审计、小程序管理、支付渠道与路由、结算渠道配置、基础设置的 read/write',
     1, 'ACTIVE', 'PLATFORM', @now, @now)
ON DUPLICATE KEY UPDATE `id` = `id`;

INSERT INTO `iam_permission_policy_item`
    (`id`, `policy_id`, `permission_id`)
VALUES
    (1000000000002000001, 1000000000001000001, 1000000000000000001),
    (1000000000002000002, 1000000000001000001, 1000000000000000002),
    (1000000000002000003, 1000000000001000001, 1000000000000000003),
    (1000000000002000004, 1000000000001000001, 1000000000000000004),
    (1000000000002000005, 1000000000001000001, 1000000000000000005),
    (1000000000002000006, 1000000000001000001, 1000000000000000006),
    (1000000000002000007, 1000000000001000001, 1000000000000000007),
    (1000000000002000008, 1000000000001000001, 1000000000000000008),
    (1000000000002000009, 1000000000001000001, 1000000000000000009),
    (1000000000002000010, 1000000000001000001, 1000000000000000010),
    (1000000000002000011, 1000000000001000001, 1000000000000000011),
    (1000000000002000012, 1000000000001000001, 1000000000000000012),
    (1000000000002000013, 1000000000001000001, 1000000000000000013),
    (1000000000002000014, 1000000000001000001, 1000000000000000014),
    (1000000000002000015, 1000000000001000001, 1000000000000000015),
    (1000000000002000016, 1000000000001000001, 1000000000000000018)
ON DUPLICATE KEY UPDATE `id` = `id`;

-- ============================================================
-- 11. 校验（执行后人工/自动化核对）
-- ============================================================
-- 预期：SELECT COUNT(*) FROM iam_permission WHERE phase = 'W2';                          => 18
-- 预期：SELECT resource, COUNT(*) FROM iam_permission GROUP BY resource;                  => 10 个资源
--       iam_user=2, iam_role=1, tenant_permission=2, mkt_permission=2, auth_audit=1,
--       mini_program=2, payment_channel=2, settlement_channel=2, platform_setting=2, personal_user=2
-- 预期：SELECT COUNT(*) FROM iam_permission_policy WHERE builtin = 1;                     => 1
-- 预期：SELECT COUNT(*) FROM iam_permission_policy_item
--        WHERE policy_id = 1000000000001000001;                                           => 16
--
-- 后续追加约定：
-- - W3-W11 各交付周迁移按 §7.7 资源清单追加对应资源 read/write 权限（phase=交付周）
-- - 关闭态功能权限（V2G 真实放电、提现等）创建时 status='INACTIVE'，经批准激活
-- - IOT 协同模式下本表数据由 IOT 推送覆盖维护（source=IOT_PUSH），平台写 API 返回
--   DEPLOYMENT_MODE_READONLY(403)；独立部署模式由平台维护
