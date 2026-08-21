# W2 前端 API 对接速查

> 版本：v1.0
> 状态：现行（对应 Step 4 API 门禁已通过的 16 个端点；后续阶段按周追加）
> 依据：[auth-v1.yaml](../../contracts/openapi/auth-v1.yaml) v1.2.0、[iam-v1.yaml](../../contracts/openapi/iam-v1.yaml) v1.2.0、[error-codes-v1.yaml](../../contracts/openapi/error-codes-v1.yaml) v1.1.0
> 读者：P1/P2 管理端前端开发。示例值均为脱敏假数据；雪花 ID 一律按字符串处理（18 位）。

## 1. 通用约定

- **Base URL**：本地 `http://localhost:8081`（`EVCO_PLATFORM_SERVER_PORT` 可覆盖）；所有路径以 `/api/v1` 开头。
- **鉴权头**：除公开端点（验证码/登录/SSO 登录/登出/刷新）外，均需 `Authorization: Bearer <accessToken>`。
- **统一响应信封**（全部端点，含错误）：

```json
{
  "code": "SUCCESS",
  "message": "操作成功",
  "data": { },
  "traceId": "b7f6c2a1e9d34f08",
  "timestamp": "2026-08-19T12:00:00.000Z"
}
```

- **错误判定**：`code != "SUCCESS"` 即失败；`data` 为 `null`。HTTP 状态码与错误码映射见 §4。
- **分页请求**：`pageNo` 从 1 起，`pageSize` 1-100；分页响应 `data` 结构为 `{ "items": [...], "pageNo": 1, "pageSize": 10, "total": 42 }`。
- **幂等头**：创建类接口（登录、创建用户）可携带 `X-Idempotency-Key`（前端生成 UUID）；同键同请求体重复提交返回首次结果，同键不同请求体返回 409 `IDEMPOTENCY_CONFLICT`。
- **时间字段**：`datetime(3)` UTC，格式 `2026-08-19T12:00:00.000`（LocalDateTime 序列化，无时区后缀）。
- **两档权限**：`GET` 需 `resource:read`；写方法需 `resource:write`；权限不足 403 `FORBIDDEN`。
- **双模式**：`data.deploymentMode` 为 `STANDALONE`（可写）或 `IOT_COLLABORATIVE`（IOT 维护域只读，写按钮一律隐藏；写接口一律 403 `DEPLOYMENT_MODE_READONLY`）。

## 2. 认证接口（8 个）

### 2.1 获取图形验证码

`GET /api/v1/auth/captcha`（公开）

```json
// 响应 data
{
  "captchaId": "c8f21e6a90b14d77",
  "imageBase64": "data:image/png;base64,iVBORw0KGgo...",
  "expiresIn": 120
}
```

前端将 `imageBase64` 直接渲染为 `<img src>`；120 秒过期、一次性使用（登录失败即作废，需重新获取）。

### 2.2 账号密码登录

`POST /api/v1/auth/login`（公开；建议带 `X-Idempotency-Key`）

```json
// 请求体
{
  "username": "admin",
  "password": "Plain#2026",
  "captchaId": "c8f21e6a90b14d77",
  "captchaCode": "A7xK",
  "tenantId": "2069700000000000001"
}
// 响应 data
{
  "accessToken": "eyJhbGciOi...",
  "refreshToken": "rt_9f8e7d6c...",
  "tokenType": "Bearer",
  "userId": "2069700000000000101",
  "displayName": "平台管理员",
  "roles": ["platform_super_admin"],
  "deploymentMode": "STANDALONE"
}
```

连续 5 次失败锁 30 分钟（锁定返回 409 `STATE_CONFLICT`，提示文案含锁定语义）。`refreshToken` 仅此一次返回，前端需安全存储。

### 2.3 刷新令牌（一次性轮换）

`POST /api/v1/auth/refresh`（公开）

```json
// 请求体
{ "refreshToken": "rt_9f8e7d6c..." }
// 响应 data：与登录响应结构一致，返回全新令牌对；旧 refreshToken 立即作废
```

旧 token 复用返回 401 `AUTH_REFRESH_TOKEN_USED`，前端应清空会话跳登录页。

### 2.4 登出

`POST /api/v1/auth/logout`（公开，凭 refreshToken 自认证）

```json
// 请求体
{ "refreshToken": "rt_9f8e7d6c..." }
// 响应 data：null
```

### 2.5 强制下线（被动撤销）

`POST /api/v1/auth/revoke`（需 `iam_user:write`）

```json
// 请求体
{ "sessionId": "2069700000000009001", "reason": "账号异常" }
// 响应 data：null
```

### 2.6 SSO 票据推送（IOT 服务间接口）

`POST /api/v1/auth/sso/tickets`（凭 `X-API-Key` 服务间鉴权；仅 IOT 协同模式启用）—— 前端不调用，列出仅为完整性。

### 2.7 SSO 免登录

`POST /api/v1/auth/sso/login`（公开；仅 IOT 协同模式启用）

```json
// 请求体
{ "ticket": "one-time-ticket-from-iot" }
// 响应 data：与登录响应结构一致
```

票据 120 秒一次性；无效/已用/过期返回 401 `SSO_TICKET_INVALID`；用户未推送返回 403 `SSO_USER_NOT_FOUND`。

### 2.8 当前用户档案（菜单与权限唯一来源）

`GET /api/v1/auth/profile`（需登录）

```json
// 响应 data
{
  "userId": "2069700000000000101",
  "username": "admin",
  "displayName": "平台管理员",
  "roles": ["platform_super_admin"],
  "permissions": ["iam_user:read", "iam_user:write", "iam_role:read"],
  "deploymentMode": "STANDALONE",
  "menus": [
    {
      "key": "system",
      "title": "系统管理",
      "path": "/system",
      "permission": null,
      "children": [
        {
          "key": "iam-user",
          "title": "平台管理员",
          "path": "/system/iam/user",
          "permission": "iam_user:read",
          "children": []
        }
      ]
    }
  ]
}
```

前端路由守卫用 `permissions` 判页面可见（`resource:read`），写按钮统一用 `resource:write` + `deploymentMode` 判显隐；菜单树由后端推导（父级自动聚合），前端只渲染。

## 3. IAM 用户与角色接口（8 个）

均按操作者租户隔离；跨租户/不存在统一 404 `IAM_USER_NOT_FOUND`。

### 3.1 分页查询用户

`GET /api/v1/iam/users?pageNo=1&pageSize=10&username=张&status=active&roleId=2069700000000002001`（需 `iam_user:read`；`displayName`/`roleId` 可选）

```json
// 响应 data
{
  "items": [
    {
      "id": "2069700000000000102",
      "username": "ops_wang",
      "displayName": "王运营",
      "status": "active",
      "roleNames": ["运营专员"],
      "tenantId": "2069700000000000001",
      "createdAt": "2026-08-18T09:30:00.000"
    }
  ],
  "pageNo": 1,
  "pageSize": 10,
  "total": 1
}
```

### 3.2 查询用户详情

`GET /api/v1/iam/users/{userId}`（需 `iam_user:read`）

```json
// 响应 data
{
  "id": "2069700000000000102",
  "username": "ops_wang",
  "displayName": "王运营",
  "status": "active",
  "version": 3,
  "roles": [
    { "id": "2069700000000002001", "name": "运营专员", "scope": "tenant", "description": "日常运营只读与处理" }
  ],
  "tenantId": "2069700000000000001",
  "createdAt": "2026-08-18T09:30:00.000",
  "updatedAt": "2026-08-19T08:12:45.000",
  "initialPassword": null
}
```

`version` 必须在编辑/删除/角色替换时原样回传（乐观锁）。

### 3.3 创建用户

`POST /api/v1/iam/users`（需 `iam_user:write`；建议带 `X-Idempotency-Key`）

```json
// 请求体
{
  "username": "ops_li",
  "displayName": "李运维",
  "roleIds": ["2069700000000002001"],
  "tenantId": "2069700000000000001"
}
// 响应 data：同 3.2，但 initialPassword 有值且仅此一次返回
{ "...": "...", "initialPassword": "Xk7mPq2vRt9z" }
```

初始密码由服务端随机生成（12 位去混淆字符），首登强制改密。

### 3.4 编辑用户

`PUT /api/v1/iam/users/{userId}`（需 `iam_user:write`）

```json
// 请求体（version 必填）
{ "displayName": "王运营-改名", "version": 3 }
// 响应 data：同 3.2
```

### 3.5 删除用户（逻辑删除）

`DELETE /api/v1/iam/users/{userId}?version=3`（需 `iam_user:write`）→ `data: null`。删除后撤销该用户全部会话。

### 3.6 替换角色绑定（乐观锁）

`PUT /api/v1/iam/users/{userId}/roles`（需 `iam_user:write`）

```json
// 请求体（roleIds 为完整目标集合；version 必填）
{ "roleIds": ["2069700000000002001", "2069700000000002002"], "version": 3 }
// 响应 data
{
  "userId": "2069700000000000102",
  "roles": [
    { "id": "2069700000000002001", "name": "运营专员", "scope": "tenant", "description": "..." },
    { "id": "2069700000000002002", "name": "客服主管", "scope": "tenant", "description": "..." }
  ]
}
```

空数组 `[]` 表示解除全部绑定。并发冲突返回 409 `VERSION_CONFLICT`，前端应重新拉详情取新 version 后让用户重试。

### 3.7 用户状态迁移

`PATCH /api/v1/iam/users/{userId}/status`（需 `iam_user:write`）

```json
// 请求体
{ "status": "locked" }
// 响应 data
{ "userId": "2069700000000000102", "status": "locked" }
```

状态机：`active→locked`、`active→disabled`、`locked→active`、`locked→disabled`、`disabled→active`；非法迁移 409 `STATE_CONFLICT`；最后一个可用平台超管受 409 `IAM_LAST_ADMIN_PROTECTED` 保护。锁定/停用立即强制下线。

### 3.8 可授予角色选项

`GET /api/v1/iam/roles/options`（需 `iam_role:read`）

```json
// 响应 data
[
  { "id": "2069700000000002001", "name": "运营专员", "scope": "tenant", "description": "日常运营只读与处理" }
]
```

`scope=platform` 表示系统租户平台级角色。创建/编辑表单的角色下拉唯一数据源。

## 4. W2 相关错误码表

| code | HTTP | 语义与前端处理 |
| --- | --- | --- |
| `SUCCESS` | 200 | 成功 |
| `VALIDATION_ERROR` | 400 | 参数校验失败；按 `message` 提示，不重试 |
| `UNAUTHENTICATED` / `AUTH_TOKEN_INVALID` / `AUTH_TOKEN_EXPIRED` | 401 | 尝试静默刷新；刷新失败清会话跳登录 |
| `AUTH_REFRESH_TOKEN_USED` | 401 | refreshToken 已用/撤销；清会话跳登录，禁止重试 |
| `FORBIDDEN` | 403 | 无权限；隐藏入口或提示无权限 |
| `DEPLOYMENT_MODE_READONLY` | 403 | IOT 协同模式写拦截；正常情况下前端已隐藏写按钮，出现即检查按钮显隐逻辑 |
| `SSO_TICKET_INVALID` | 401 | SSO 票据无效/已用/过期；回 IOT 重新发起 |
| `SSO_USER_NOT_FOUND` | 403 | 用户未推送；提示先在 IOT 侧创建 |
| `IAM_USER_NOT_FOUND` / `RESOURCE_NOT_FOUND` | 404 | 资源不存在或不可见；回列表刷新 |
| `IDEMPOTENCY_CONFLICT` | 409 | 幂等键复用但请求体不同；更换幂等键 |
| `VERSION_CONFLICT` | 409 | 乐观锁冲突（可重试）；重拉详情取新 version |
| `STATE_CONFLICT` | 409 | 状态机非法迁移或账号锁定；提示当前状态 |
| `IAM_USERNAME_DUPLICATE` | 409 | 用户名已存在；表单校验提示 |
| `IAM_LAST_ADMIN_PROTECTED` | 409 | 最后平台超管保护；提示不可操作 |
| `IAM_ROLE_OUT_OF_SCOPE` | 403 | 角色越权授予；刷新角色选项 |
| `INTERNAL_ERROR` | 500 | 系统错误；可重试，附 `traceId` 反馈 |

## 5. 前端对接要点

1. **令牌流**：登录 → 存 accessToken/refreshToken → 401 时调 `/refresh` 轮换 → 成功重放原请求，失败清会话。
2. **写按钮显隐**：`profile.permissions` 含 `xxx:write` 且 `deploymentMode === "STANDALONE"` 才显示；IOT 协同模式写按钮一律隐藏。
3. **菜单渲染**：仅渲染 `profile.menus` 树（后端已按权限推导），前端不做二次过滤；架构调整由后端菜单配置承接。
4. **乐观锁**：所有携带 `version` 的写操作遇 409 `VERSION_CONFLICT` 时，重新 GET 详情后再提交，禁止用旧 version 重试。
5. **验证码**：每次登录尝试前获取新验证码；失败后旧验证码作废。

## 修订记录

| 时间 | 版本 | 原因 |
| --- | --- | --- |
| 2026-08-19 | v1.0 | W2 Step 4 门禁通过后面向前端输出 16 端点对接速查（auth 8 + iam 8）。 |
