# W2 API 权限与菜单对应检查报告

> 版本：v1.0
>
> 状态：待用户审核。
>
> 编制日期：2026-08-20
>
> 检查委托（用户原话）：「前端开工之前，帮我检查 W2 API 接口文档，帮我查看下权限是否和菜单目录结构对应，还有 api 接口文档 创建用户指的是超级管理员用户还是什么。」
>
> 性质：用户委托的前端开工（Step 5）前一致性检查；**不替代 Step 4「API 门禁」评审纪要，也不构成 Step 5 放行决定**——Step 5 是否开工仍由用户按《开发流程总纲》批准。
>
> 业务基准：[《充电运营平台业务分析报告 v1》](../../docs/requirements/充电运营平台业务分析报告-v1.md) §2 三级权限与资产体系（2026-08-20 用户澄清的权威业务事实：平台级仅平台管理员＝IOT 超级管理员；租户级/企业级各分管理员＋普通用户；权限逐级子集授予）。

---

## 1. 检查范围与方法

**四方核对**：权限初始化 DDL（`V202608240005__create_permission_and_role_permission.sql` 的 `menu_path`/权限码）↔ 菜单架构 v4.1（W2 范围菜单）↔ [W2 页面功能详细清单 v1.3](../../docs/page-specs/W2-页面功能详细清单-v1.md)（路由/权限码/按钮）↔ 接口规范与人读契约（[IAM 用户与角色接口规范 v1.2](../../docs/api/IAM用户与角色接口规范-v1.md)、[P1 配置接口规范 v1](../../docs/api/P1配置接口规范-v1.md)、[个人用户主档接口规范 v1](../../docs/api/个人用户主档接口规范-v1.md)、[W2 前端 API 对接速查 v1](../../docs/api/W2-前端API对接速查-v1.md)、iam-v1.yaml v1.2.0）。

W2 范围（按 12 周计划覆盖矩阵）：个人用户（主档部分）、小程序管理、支付渠道与路由、结算渠道配置、平台管理员、租户权限分配、授权审计、基础设置、营销权限（W2 授权模型部分），共 9 个菜单。

## 2. 结论一：权限与菜单目录结构对应核查 —— **通过**

### 2.1 W2 九菜单权限码四方对照表

| # | 菜单（架构 v4.1 命名） | 页面路由（页面清单 v1.3） | 权限码（DDL 初始化，scope=PLATFORM） | 接口规范/速查一致性 |
| --- | --- | --- | --- | --- |
| 1 | 系统与配置>权限管理>平台管理员 | `/system-config/permission-management/platform-admin` | `iam_user:read` / `iam_user:write` | ✅ IAM 规范 §1.1、速查一致 |
| 2 | 系统与配置>权限管理>租户权限分配 | `/system-config/permission-management/tenant-permission` | `tenant_permission:read` / `tenant_permission:write` | ✅ 对应 `iam_tenant_grant` 表（DDL 注释明确） |
| 3 | 系统与配置>权限管理>营销权限 | `/system-config/permission-management/marketing-permission` | `mkt_permission:read` / `mkt_permission:write` | ✅ W2 授权模型部分；W9 业务权限与审批后续追加 |
| 4 | 系统与配置>权限管理>授权审计 | `/system-config/permission-management/auth-audit` | `auth_audit:read`（纯查看，无 write） | ✅ 纯查看页无写按钮，符合两档模型 |
| 5 | 系统与配置>应用与内容>小程序管理 | `/system-config/app-content/mini-program` | `mini_program:read` / `mini_program:write` | ✅ P1 配置规范 §2 一致 |
| 6 | 系统与配置>交易配置>支付渠道与路由 | `/system-config/trade-config/payment-channel` | `payment_channel:read` / `payment_channel:write` | ✅ 同上 |
| 7 | 系统与配置>交易配置>结算渠道配置 | `/system-config/trade-config/settlement-channel` | `settlement_channel:read` / `settlement_channel:write` | ✅ 同上 |
| 8 | 系统与配置>基础设置>基础设置 | `/system-config/base-setting/platform-setting` | `platform_setting:read` / `platform_setting:write` | ✅ 同上 |
| 9 | 客户与权益>个人客户>个人用户 | `/customer-equity/personal-customer/personal-user`（冲突处置子页 `{id}/conflicts`） | `personal_user:read` / `personal_user:write` | ✅ 个人用户主档规范 §1.1 一致（POST 例外见 F4） |

辅助权限（无独立菜单，挂平台管理员页角色 Tab/下拉）：`iam_role:read` → 同菜单路径 1。✅ 符合「平台角色 Tab 在平台管理员菜单内」的架构约定。

### 2.2 核查结论

1. **9 个 W2 菜单全部有对应权限码，`menu_path` 与页面清单路由逐字一致**，无缺权限的菜单、无无主的权限码（W2 共 10 资源 18 条权限，DDL 与 W2 技术设计 §7.7/§7.8 清单一致）。
2. **鉴权规则一致**：GET 校验 `:read`、POST/PUT/PATCH/DELETE 校验 `:write`，各规范 §1.1 表述统一；授权审计纯查看页仅 read，符合模型。
3. **菜单可见性机制一致**：菜单树由 `/api/v1/auth/profile` 的 `menus` 后端推导返回（速查含示例），父级不单独授权、按叶子 `resource:read` 自动推导——与业务分析报告 §2.5 一致。
4. **双模式只读声明完整**：iam-v1.yaml 全部写端点（POST/PUT/DELETE/PATCH users、roles、status）均声明 IOT 协同模式 403 `DEPLOYMENT_MODE_READONLY`；读端点不受影响。判定顺序（登录态→权限→模式拦截）在 IAM 规范 §1.2 明确。
5. 页面清单按钮权限统一 `resource:write`（不逐按钮设码），与两档模型及页面级按钮规则一致。

## 3. 结论二：「创建用户」语义裁决

### 3.1 裁决（直接回答）

**W2 接口文档（iam-v1.yaml / IAM 用户与角色接口规范）中的「新增管理用户 `POST /api/v1/iam/users`」创建的是平台级管理用户——即平台管理员（用户口径中的"IOT 超级管理员"体系的平台管理员账号），不是租户管理员、企业用户，更不是 M1 个人用户。**

依据：
1. 挂载菜单为「系统与配置>权限管理>平台管理员」，权限码 `iam_user:write`，DDL 中 `scope='PLATFORM'`、`menu_path` 指向平台管理员页。
2. 2026-08-20 用户澄清权威事实：**平台级仅平台管理员一种角色**（无平台普通用户）；平台管理员拥有全租户权限，在系统与配置>权限管理下维护。
3. 双模式行为：IOT 协同模式下平台管理员由 IOT 推送（source=IOT_PUSH、iot_user_id 识别、SSO 免登录），本接口返回 403 只读；独立部署模式下由平台管理员创建/维护平台管理员账号。
4. 契约已为租户级预留（`CreateUserRequest.tenantId`「平台管理员可省略」、`RoleOptionVO.scope` 枚举含 `tenant`、用户名「租户范围内唯一」）——**该预留的实际使用入口是 W7 的「租户管理」菜单**（平台管理员代维护租户组织/角色/用户，或租户管理员自维护），不在 W2 页面（见 F2）。

### 3.2 三级用户与个人用户的创建入口全景（对照三级体系）

| 用户类型 | 创建/维护入口 | 交付周 | 契约落点 |
| --- | --- | --- | --- |
| 平台管理员 | P1「平台管理员」菜单：`POST /api/v1/iam/users`（省略 `tenantId`） | **W2** | iam-v1.yaml（本次检查对象） |
| 租户管理员 / 租户普通用户 | P1「租户管理」菜单（客户与权益>合作运营）：平台管理员代维护，或租户管理员自维护本租户 | W7 | W7 契约（可复用 iam/users ＋ tenantId，或独立租户用户端点，W7 Step 2 定） |
| 企业管理员 / 企业普通用户 | 企业实体由平台管理员或被授权该页面的租户创建（P1「企业管理」）；企业成员由企业管理员在 P2「企业组织管理」维护（不超租户授权范围） | W7 | W7 契约（企业角色 scope 需补 enterprise，见 F3） |
| 个人用户（M1） | **不存在运营手工创建**：渠道登录服务端幂等创建/更新主档；P1「个人用户」菜单仅查看、编辑资料与冲突人工处置 | W2 | personal-identity-v1.yaml |

## 4. 发现的问题与处置建议

> 按《项目记忆》教训「文档不一致先出清单供用户审核，不可直接修复」，以下问题**均未直接修改**，处置方式待用户批准。

| # | 级别 | 问题 | 证据 | 处置建议 |
| --- | --- | --- | --- | --- |
| F1 | P2（建议 Step 5 前修订文档） | IAM 规范 §1 称 iam_user 是「平台管理员、租户管理员、**运营人员**等受授权后台主体」——与权威事实「平台级仅平台管理员、无平台普通用户」冲突（"运营人员"易被读作平台级普通用户） | IAM 用户与角色接口规范 v1.2 §1 | 修订表述为「平台管理员、租户管理员、租户普通用户等各级受授权管理主体（企业级成员账号随 W7 交付）」；同步检查 iam-v1.yaml info.description |
| F2 | P2（建议 Step 5 前澄清） | `CreateUserRequest.tenantId`（"平台管理员可省略"）未标注交付边界——若 W2 前端在平台管理员页提供"所属租户"选择，将与「租户用户由 W7 租户管理菜单维护」的业务事实冲突 | iam-v1.yaml `CreateUserRequest.tenantId` | 在 tenantId description 补充：「W2 平台管理员页面创建平台级用户时必须省略；租户级用户创建入口随 W7 租户管理交付」；页面清单平台管理员页表单确认**无租户字段**（当前 v1.3 表单字段已无 tenantId，仅需契约侧补注） |
| F3 | P3（登记 W7 待办） | `RoleOptionVO.scope` 枚举仅 `[platform, tenant]`，缺 `enterprise`；而权限层级为 PLATFORM/TENANT/ENTERPRISE，W7 企业角色若共用 iam_role 模型则枚举不足 | iam-v1.yaml `RoleOptionVO` | W7 Step 2 契约修订时补 `enterprise` 并扩展 roles/options 返回范围；W2 不受影响 |
| F4 | P2（建议 Step 5 前澄清） | `POST /api/v1/personal-users` 标注权限码 `personal_user:write`，但语义为「渠道登录幂等创建主档」——渠道登录是 M1 服务端内部动作（授权码服务端换取、不信任客户端），不存在持有 `personal_user:write` 的调用方；若视为运营手工开户入口，又与「M1 主档由渠道登录自动创建、P1 不手工开户」的业务规则冲突 | 个人用户主档接口规范 v1 §2 第 2 行；业务规则基线 §3.2/3.3 | 澄清该端点定位：主档创建应为登录链路内部逻辑（不暴露管理端权限注解），从管理端接口表移除或改为「服务内部（登录链路）」标注；管理端保留 PUT 编辑与 PATCH 冲突处置 |
| F5 | 观察（不阻塞） | 「租户权限分配」W2 交付页面＋权限＋`iam_tenant_grant` 表，但独立模式下租户实体 W7 才存在，W2 页面暂无可操作对象（IOT 协同模式租户由 IOT 推送） | 12 周计划 W3/W7 排期 | 属分阶段交付正常现象；前端按页面清单空态规范处理「暂无租户」空态即可 |
| F6 | 观察（确认无问题） | `iam_role:read` 与 `iam_user:read/write` 共用同一 `menu_path`（平台管理员页内角色 Tab 与下拉） | DDL 初始化第 3 行 | 符合「同一对象近邻配置放三级菜单内 Tab」的架构约定，非问题 |

## 5. 前端开工（Step 5）建议

1. **权限与菜单对应无阻塞缺口**，四方一致（§2）；「创建用户」语义已裁决为平台级管理用户（§3），前端表单按页面清单 v1.3 执行（无租户字段、写按钮统一 `iam_user:write`＋双模式隐藏）。
2. F1/F2/F4 三项 P2 为**文档/契约表述修订**（约 3 处文件、不动代码与 DDL），建议 Step 5 开工前修订完毕，避免前端按歧义表述实现；F3 登记 W7；F5 空态处理纳入页面实现。
3. Step 5 正式开工仍需用户按《开发流程总纲》授权；本报告作为授权前的事实输入。

## 修订记录

| 时间 | 版本 | 修订原因 |
| --- | --- | --- |
| 2026-08-20 | v1.0 | 首版：W2 九菜单权限四方核对通过；「创建用户」裁决为平台级管理用户；登记 F1-F6 六项发现（F1/F2/F4 建议 Step 5 前修订，F3 转 W7，F5/F6 观察）。 |
