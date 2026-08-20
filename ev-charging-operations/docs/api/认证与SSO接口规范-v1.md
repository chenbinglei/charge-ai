# 认证与 SSO 接口规范

> 版本：v1.2
> 状态：W2 模块实施前的强制接口设计基线；本文不是已发布运行时 API。
> 依据：[auth-v1.yaml](../../contracts/openapi/auth-v1.yaml) v1.2.0（全部 200 响应统一 ApiResponse 信封；m1/wechat-mock 端点标注交付批次依赖）、《[API 通用接口规范](API通用接口规范-v1.md)》、《[W2 技术设计与契约包](../design/W2-IAM个人身份与配置技术设计与契约包.md)》§8 双模式设计。
> 边界：覆盖 P1/P2 管理端认证（登录、图形验证码、SSO 免登录、profile）；M1 个人用户微信登录见 auth-v1.yaml 的 m1/wechat-mock 端点，不在本文展开。

## 1. 资源边界

- 认证服务归 `platform-service`；令牌签发/刷新/撤销、图形验证码、SSO ticket 均由其承担，`api-gateway` 仅做令牌预校验与服务间凭证校验，不做认证决策。
- 图形验证码与 SSO ticket 均存 Redis（验证码 TTL 120 秒、ticket TTL 120 秒），一次性使用，不落库。
- SSO 免登录仅在 `deployment_mode=IOT_COLLABORATIVE` 时启用；独立部署模式下 `/api/v1/auth/sso/*` 入口关闭。
- SSO 仅识别已推送用户：用户须先在 IOT 侧创建并由 IOT 推送到 `iam_user`（source=IOT_PUSH）；P1 不做 JIT 即时开户。

## 2. 统一资源接口

| 方法与路径 | 权限 | 请求模型 | 响应模型 | 中文语义与约束 |
| --- | --- | --- | --- | --- |
| `GET /api/v1/auth/captcha` | 公开 | 无 | `ApiResponse<CaptchaResponse>` | 获取图形验证码（captchaId + Base64 PNG，4-6 位字母数字，TTL 120 秒）。 |
| `POST /api/v1/auth/login` | 公开 | `LoginRequest` + `X-Idempotency-Key` | `ApiResponse<LoginResponse>` | 用户名 + 密码 + 图形验证码登录；验证码一次性校验；5 次失败锁定 30 分钟。 |
| `POST /api/v1/auth/sso/tickets` | 服务间凭证（X-API-Key/mTLS） | `SsoTicketPushRequest` | `ApiResponse<Void>` | IOT 后端推送一次性 ticket（≥32 字符随机串）与 iotUserId；仅 IOT 协同模式可用。 |
| `POST /api/v1/auth/sso/login` | 公开（凭 ticket） | `SsoLoginRequest` | `ApiResponse<LoginResponse>` | ticket 换会话；校验存在、未使用、未过期后按 iotUserId 定位本地用户签发令牌。 |
| `GET /api/v1/auth/profile` | 登录态 | 无 | `ApiResponse<ProfileResponse>` | 当前用户档案：基础信息、全部权限码、deploymentMode、后端推导的可见菜单树。 |
| `POST /api/v1/auth/logout` | 登录态 | `LogoutRequest` | `ApiResponse<Void>` | 登出并撤销 refresh_token。 |
| `POST /api/v1/auth/refresh` | 凭 refresh_token | `RefreshRequest` | `ApiResponse<LoginResponse>` | 刷新 access_token；refresh_token 一次性使用。 |
| `POST /api/v1/auth/revoke` | 登录态 | `RevokeRequest` | `ApiResponse<Void>` | 管理员强制下线指定会话。 |

## 3. 关键请求与响应字段

| 模型/字段 | 类型 | 中文说明 | 约束 |
| --- | --- | --- | --- |
| `LoginRequest.captchaId` | string | 图形验证码标识 | 必填；来自 `GET /api/v1/auth/captcha`。 |
| `LoginRequest.captchaCode` | string | 图形验证码内容 | 必填；4-6 位，大小写不敏感，一次性校验。 |
| `SsoTicketPushRequest.ticket` | string | 一次性票据 | 必填；≥32 字符随机串；TTL 120 秒；一次性使用。 |
| `SsoTicketPushRequest.iotUserId` | string | IOT 侧用户 ID | 必填；P1 按 `iam_user.iot_user_id`（source=IOT_PUSH）定位本地用户。 |
| `SsoLoginRequest.ticket` | string | 跳转 URL 携带的票据 | 必填；校验通过后立即失效（防重放）。 |
| `LoginResponse.deploymentMode` | string | 部署模式 | `IOT_COLLABORATIVE` / `STANDALONE`；前端据此控制 IOT 维护数据域写按钮显隐。 |
| `ProfileResponse.permissions` | array<string> | 全部权限码 | 格式 `resource:read` / `resource:write`；供前端路由守卫与按钮显隐。 |
| `ProfileResponse.menus` | array<MenuNode> | 可见菜单树 | 后端推导：叶子可见=拥有 `resource:read`；父级可见=其下存在可见叶子；全无则整棵隐藏。 |

## 4. SSO 免登录交互流程（IOT 协同模式）

1. 用户在 IOT 平台点击「充电运营平台」入口。
2. IOT 后端生成一次性 ticket，调用 `POST /api/v1/auth/sso/tickets` 推送到 P1（服务间凭证认证）；P1 存 Redis（TTL 120 秒）。
3. IOT 前端重定向浏览器到 P1 前端 `/sso/login?ticket=xxx`。
4. P1 前端自动调用 `POST /api/v1/auth/sso/login`；成功后建立与密码登录完全一致的会话，直接进入平台首页（用户无感知）。
5. 失败处理：ticket 无效/过期 → `SSO_TICKET_INVALID`(401)，前端展示失效页并提供「返回 IOT 平台」链接；本地无此用户或不可用 → `SSO_USER_NOT_FOUND`(403)，提示联系 IOT 管理员。
6. 审计：SSO 登录在 auth_session 记录 `auth_type=SSO` 与来源 IP，写入操作日志。

### 4.1 ticket 防重放实现策略（Step 3 编码依据）

**Redis 存储结构**

| 项 | 值 | 说明 |
| --- | --- | --- |
| Key | `sso:ticket:{ticket}` | ticket 原文作 key；仅此一处持有 ticket 值 |
| Value | JSON：`{"iotUserId":..., "displayName":"...", "issuedAt":"..."}` | 换会话所需最小载荷；不含密码/令牌材料 |
| TTL | 120 秒（`EX 120`） | 与 `SsoTicketPushRequest.expireAt` 一致；Redis 过期即自动作废，无需清理任务 |
| 写入命令 | `SET sso:ticket:{ticket} <payload> EX 120 NX` | `NX` 保证唯一性：key 已存在（ticket 撞号）时推送失败返回 `VALIDATION_ERROR`，不覆盖、不续期 |

**一次性消费（防重放核心）**

- 校验命令：`GETDEL sso:ticket:{ticket}`（Redis ≥6.2；低版本用等价 Lua 脚本 `GET` + `DEL` 原子执行）。
- 原子性保证：`GETDEL` 读取与删除一步完成。两个并发请求携带同一 ticket 时，**有且仅有一个**能读到 payload，另一个读到 nil → 返回 `SSO_TICKET_INVALID`(401)。
- 判定顺序：读到 nil 即失败，无法区分「不存在 / 已使用 / 已过期」——**对外统一报 `SSO_TICKET_INVALID`，错误信息不泄露 ticket 生命周期状态**（防止探测）。
- 消费后不回滚：即使后续用户定位失败（`SSO_USER_NOT_FOUND`），ticket 也保持已删除——重放者不能通过制造失败反复试探。

**生成侧约束（IOT 侧，契约约定）**

- ticket 为 ≥32 字符随机串（CSPRNG 生成，如 32 字节随机数 Base64/Hex 编码）；熵足够使撞号概率可忽略。
- ticket 仅出现在两个位置：IOT→P1 推送请求体、P1 前端跳转 URL 查询参数；**不写日志、不进审计明文**（审计只记 ticket 指纹 = SHA-256 前 8 位）。

**降级策略**

- Redis 不可用时 SSO 推送/登录整体失败（fail-closed），不降级为本地内存缓存（多实例下无法保证一次性语义）。
- 令牌签发、会话建立与密码登录共用同一套 `auth_session`/`refresh_token` 逻辑，无 SSO 特有分支。

## 5. 菜单可见性推导规则（profile.menus）

- 菜单树配置存放于 `platform-service` 配置文件（对齐《充电运营平台菜单架构》v4.1），架构调整（菜单升降级、层级变化）仅改配置与权限表 `menu_path` 映射，不改代码；待前端骨架页确定后再评估是否入库。
- 叶子菜单（页面）可见 = 用户拥有对应 `resource:read` 权限。
- 一级/二级父级菜单不单独授权：其下存在任意可见叶子即显示；全部叶子不可见则父级整棵隐藏，不出现空菜单。
- 前端路由守卫：访问 `path` 不在返回菜单树中的页面时重定向 403 页。

## 6. 后端、前端与测试落地

- 后端目录固定为域包直挂 `auth/{controller,dto,vo,service,service/impl,cache}`（无 `module/` 中间层）；验证码生成与校验、ticket 接收与校验、菜单推导分别在 `captcha`、`sso`、`menuprovider` 组件实现；Controller 不得直连 Redis。
- 前端：登录页集成图形验证码组件（点击图片刷新）；`/sso/login` 为独立轻量页面（仅处理 ticket 换会话与失败态）；全局 store 缓存 profile（permissions + deploymentMode + menus）。
- 测试覆盖："验证码生成/过期/一次性、登录验证码错误、5 次锁定、SSO ticket 推送鉴权、ticket 换会话、**ticket 重放拒绝（GETDEL 原子性：并发同 ticket 仅一个成功）、ticket 撞号 NX 拒绝、用户定位失败后 ticket 不复活**、用户未推送拒绝、非 IOT 模式入口关闭、Redis 不可用 fail-closed、profile 菜单推导（部分权限父级隐藏/全无整棵隐藏）"；每个测试类、方法、夹具和关键 Given/When/Then 代码块写中文说明。

## 修订记录

| 时间 | 版本 | 修订原因 |
| --- | --- | --- |
| 2026-08-19 | v1.0 | 建立认证与 SSO 接口基线：确认登录使用图形验证码；新增 IOT 协同模式一次性 ticket 免登录（仅识别已推送用户）；新增 profile 返回权限码、部署模式与父级自动推导的可见菜单树。 |
| 2026-08-19 | v1.1 | 新增 §4.1 ticket 防重放实现策略：Redis 键结构（sso:ticket:{ticket}，TTL 120 秒）、SET NX 唯一性写入、GETDEL 原子一次性消费（并发仅一个成功）、失败不区分生命周期状态、消费后不回滚、ticket 不入日志（审计只记指纹）、Redis 故障 fail-closed；测试项补充并发重放/撞号/不复活用例。 |
| 2026-08-20 | v1.2 | 06-文档不一致清单 C-5：依据行同步 auth-v1.yaml v1.2.0（全部 200 响应统一 ApiResponse 信封、m1/wechat-mock 端点标注交付批次依赖）；后端目录改为域包直挂 `auth/{controller,dto,vo,service,service/impl,cache}`；修正头部版本号与修订记录脱节（v1.1 修订时未更新头部）。 |
