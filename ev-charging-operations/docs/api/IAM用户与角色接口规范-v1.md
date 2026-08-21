# IAM 用户与角色接口规范

> 版本：v1.3
> 状态：W2 模块实施前的强制接口设计基线；本文不是已发布运行时 API。
> 依据：DEC-20260814-021、《[API 通用接口规范](API通用接口规范-v1.md)》和现行 12 周计划。
> 边界：仅覆盖 P1/P2 等管理端的 IAM 管理用户与角色绑定；M1 个人充电用户不配置页面或功能角色。

## 1. 资源边界

- `iam user` 是平台管理员、租户管理员、租户普通用户等各级受授权管理主体（企业级成员账号随 W7 交付）；**平台级仅平台管理员一种角色**（2026-08-20 用户确认，不存在平台普通用户/运营人员角色）；其角色、数据范围、状态和审计由 IAM 模块维护。
- M1 个人用户的渠道身份、业务资格、订单、钱包和权益属于个人用户主档与业务规则，不得复用本接口配置页面/功能角色。
- 用户与角色为多对多关系；用户角色的新增、替换、禁用和删除都必须保留操作审计，并执行租户/平台数据范围校验。

### 1.1 鉴权模型（资源 + 读/写两档）

- 权限模型为「资源 + 读/写两档」，与 IOT linkos 对齐；不再区分菜单/按钮/API 三级权限，同一资源的权限码只有 `resource:read` 与 `resource:write` 两档。
- 本接口涉及的权限码：`iam_user:read`（查询）、`iam_user:write`（新增/编辑/删除/角色替换/状态变更）、`iam_role:read`（角色下拉/选项）。
- 鉴权规则：`GET` 接口校验 `resource:read`；`POST`/`PUT`/`PATCH`/`DELETE` 接口校验 `resource:write`；权限不足统一返回 `FORBIDDEN`(403)。
- 后端通过注解声明权限码（如 `@HasPermission("iam_user:write")`），网关不做对象级授权，权限判定在 `platform-service` 完成。

### 1.2 双模式写拦截（IOT 协同 / 独立部署）

- 平台部署模式（`deployment_mode`：`IOT_COLLABORATIVE` / `STANDALONE`）部署时二选一，运行期不变。
- **IOT 协同模式**：平台管理员（iam_user）与平台权限/角色等 IOT 维护数据域由 IOT 通过 HTTP 推送，平台只读。本接口中所有写操作（`POST`/`PUT`/`PATCH`/`DELETE`）一律返回 `DEPLOYMENT_MODE_READONLY`(403)；查询接口不受影响。
- **独立部署模式**：本接口按权限正常读写，无额外拦截。
- 判定顺序：先校验登录态 → 再校验 `resource:write` 权限 → 最后校验部署模式写拦截；模式拦截错误码使用 `DEPLOYMENT_MODE_READONLY`，与权限不足的 `FORBIDDEN` 区分。

## 2. 统一资源接口

| 方法与路径 | 权限码 | 请求模型 | 响应模型 | 中文语义与约束 |
| --- | --- | --- | --- | --- |
| `GET /api/v1/iam/users` | `iam_user:read` | `UserPageQuery` | `ApiResponse<PageResponse<UserListVO>>` | 按用户名、姓名、状态、角色和数据范围分页查询；不得返回密码、令牌或敏感认证材料。 |
| `GET /api/v1/iam/users/{userId}` | `iam_user:read` | 路径参数 `userId` | `ApiResponse<UserDetailVO>` | 查询单一可见用户及受控角色摘要。 |
| `POST /api/v1/iam/users` | `iam_user:write` | `CreateUserRequest` + `X-Idempotency-Key` | `ApiResponse<UserDetailVO>` | 新增管理用户并绑定初始角色；用户名在所属租户范围内唯一。**W2 语境下创建的是平台级管理用户（平台管理员账号，`tenantId` 必须省略）；租户级用户创建入口随 W7 租户管理菜单交付**。IOT 协同模式返回 `DEPLOYMENT_MODE_READONLY`。 |
| `PUT /api/v1/iam/users/{userId}` | `iam_user:write` | `UpdateUserRequest` | `ApiResponse<UserDetailVO>` | 编辑允许维护的资料与版本；不通过本接口重置认证凭据。IOT 协同模式返回 `DEPLOYMENT_MODE_READONLY`。 |
| `PUT /api/v1/iam/users/{userId}/roles` | `iam_user:write` | `ReplaceUserRolesRequest`（含 `version`） | `ApiResponse<UserRoleBindingVO>` | 以完整角色集合替换绑定；需校验操作者不可越权授予角色；携带 `version` 乐观锁，并发替换冲突返回 409 `VERSION_CONFLICT`。IOT 协同模式返回 `DEPLOYMENT_MODE_READONLY`。 |
| `PATCH /api/v1/iam/users/{userId}/status` | `iam_user:write` | `ChangeUserStatusRequest` | `ApiResponse<UserStatusVO>` | 启用、停用或锁定用户；不得停用最后一个可用平台超级管理员。IOT 协同模式返回 `DEPLOYMENT_MODE_READONLY`。 |
| `DELETE /api/v1/iam/users/{userId}` | `iam_user:write` | 路径参数 `userId` + `version` | `ApiResponse<Void>` | 逻辑删除；保留审计与历史业务关联，禁止物理删除。IOT 协同模式返回 `DEPLOYMENT_MODE_READONLY`。 |
| `GET /api/v1/iam/roles/options` | `iam_role:read` | 可选范围参数 | `ApiResponse<List<RoleOptionVO>>` | 仅返回当前操作者可授予的角色，供受控下拉选择。 |

## 3. 关键请求与响应字段

| 模型/字段 | 类型 | 中文说明 | 约束 |
| --- | --- | --- | --- |
| `CreateUserRequest.username` | string | 管理用户登录名 | 必填；租户范围内唯一；禁止手机号、身份证等敏感字段作默认用户名。 |
| `CreateUserRequest.displayName` | string | 后台展示姓名 | 必填；按中文姓名/组织展示规则脱敏。 |
| `CreateUserRequest.roleIds` | array<string> | 初始绑定角色标识集合 | 至少一个；全部必须在操作者可授予范围内；雪花 ID 统一序列化为字符串。 |
| `UpdateUserRequest.version` | number | 乐观锁版本 | 必填；冲突返回 `VERSION_CONFLICT`。 |
| `ReplaceUserRolesRequest.roleIds` | array<string> | 替换后的完整角色集合 | 必填；雪花 ID 统一序列化为字符串；空集合仅在明确允许无角色保留账号时使用。 |
| `ReplaceUserRolesRequest.version` | number | 乐观锁版本 | 必填；与其他写端点一致；并发替换冲突返回 409 `VERSION_CONFLICT`。 |
| `ChangeUserStatusRequest.status` | string | 目标账户状态 | 枚举为 `active`/`locked`/`disabled`；状态迁移以 IAM 状态机为准（`active → locked` 锁定、`locked → active` 解锁、`active/locked → disabled` 停用、`disabled → active` 恢复需审计；最后管理员保护对应 `active → disabled` 受保护）。 |
| `UserListVO.roleNames` | array<string> | 可见角色名称摘要 | 只返回操作者有权查看的角色信息。 |
| `UserDetailVO.version` | number | 用户当前并发版本 | 前端编辑/删除时原样回传。 |
| `UserDetailVO.roles` | array | 角色绑定明细 | 不返回角色内部策略、密钥或不属于当前范围的数据。 |

## 4. 后端、前端与测试落地

- 后端目录固定为域包直挂 `iam/{controller,dto,vo,entity,mapper,service,service/impl,cache,util}`（无 `module/` 中间层）；角色替换、状态迁移和越权授予校验由 `service` 编排，`validator`/`statemachine`/`policy` 为规模化后演进方向，Controller 不得直连 Mapper。
- 权限校验通过 `@HasPermission("iam_user:read|iam_user:write")` 注解声明在 Controller 方法上；部署模式写拦截在 `service` 层统一收口（读 `deployment_mode` 配置，IOT 协同模式下对 IOT 维护数据域的写方法抛出 `DEPLOYMENT_MODE_READONLY`）。
- 前端目录固定为业务模块组织，例如 `views/iam/user/`、`api/iam/user.ts`、`stores/iam-user.ts`；列表、编辑、绑定角色、停用和删除均处理加载、空、失败、无权限、版本冲突与关闭态；IOT 协同模式下写按钮一律隐藏（依据 `/api/v1/auth/profile` 返回的 `deployment_mode`）。
- 测试按“查询数据范围、创建幂等、用户名重复、角色越权、编辑与角色替换乐观锁冲突（409 `VERSION_CONFLICT`）、最后管理员保护、逻辑删除不可见、审计字段完整、两档权限（read 可查不可写、write 全量）、IOT 协同模式写拦截返回 `DEPLOYMENT_MODE_READONLY`”逐项覆盖；每个测试类、方法、夹具和关键 Given/When/Then 代码块写中文说明。

## 修订记录

| 时间 | 版本 | 修订原因 |
| --- | --- | --- |
| 2026-08-14 | v1.0 | 建立用户与角色的统一资源接口边界，防止管理用户 IAM 与 M1 个人用户授权混用。 |
| 2026-08-19 | v1.1 | 对齐两档权限模型（resource:read/write）与双模式设计：接口表新增权限码列，新增 §1.1 鉴权模型与 §1.2 双模式写拦截（`DEPLOYMENT_MODE_READONLY`），测试项补充两档权限与模式拦截覆盖。 |
| 2026-08-20 | v1.2 | 06-文档不一致清单 A-6/C-1~C-4：对齐 iam-v1.yaml v1.2.0——`ReplaceUserRolesRequest` 补必填 `version` 乐观锁与 409 `VERSION_CONFLICT`；`roleIds` 类型由 `array<number>` 改为 `array<string>`（雪花 ID 统一序列化为字符串）；后端目录改为域包直挂 `iam/{controller,dto,vo,entity,mapper,service,service/impl,cache,util}`；测试项补角色替换乐观锁冲突。 |
| 2026-08-20 | v1.3 | W2 API 检查报告 F1/F2 处置（用户批准）：§1 「运营人员」表述修正为「平台管理员、租户管理员、租户普通用户等各级受授权管理主体（企业级随 W7 交付）」并固化「平台级仅平台管理员」权威事实；§2 POST 行明确 W2 创建的是平台级管理用户（`tenantId` 必须省略）、租户级入口随 W7 租户管理交付。 |
