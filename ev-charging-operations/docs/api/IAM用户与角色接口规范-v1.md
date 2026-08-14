# IAM 用户与角色接口规范

> 版本：v1.0
> 状态：W2 模块实施前的强制接口设计基线；本文不是已发布运行时 API。
> 依据：DEC-20260814-021、《[API 通用接口规范](API通用接口规范-v1.md)》和现行 12 周计划。
> 边界：仅覆盖 P1/P2 等管理端的 IAM 管理用户与角色绑定；M1 个人充电用户不配置页面或功能角色。

## 1. 资源边界

- `iam user` 是平台管理员、租户管理员、运营人员等受授权后台主体；其角色、数据范围、状态和审计由 IAM 模块维护。
- M1 个人用户的渠道身份、业务资格、订单、钱包和权益属于个人用户主档与业务规则，不得复用本接口配置页面/功能角色。
- 用户与角色为多对多关系；用户角色的新增、替换、禁用和删除都必须保留操作审计，并执行租户/平台数据范围校验。

## 2. 统一资源接口

| 方法与路径 | 请求模型 | 响应模型 | 中文语义与约束 |
| --- | --- | --- | --- |
| `GET /api/v1/iam/users` | `UserPageQuery` | `ApiResponse<PageResponse<UserListVO>>` | 按用户名、姓名、状态、角色和数据范围分页查询；不得返回密码、令牌或敏感认证材料。 |
| `GET /api/v1/iam/users/{userId}` | 路径参数 `userId` | `ApiResponse<UserDetailVO>` | 查询单一可见用户及受控角色摘要。 |
| `POST /api/v1/iam/users` | `CreateUserRequest` + `X-Idempotency-Key` | `ApiResponse<UserDetailVO>` | 新增管理用户并绑定初始角色；用户名在所属租户范围内唯一。 |
| `PUT /api/v1/iam/users/{userId}` | `UpdateUserRequest` | `ApiResponse<UserDetailVO>` | 编辑允许维护的资料与版本；不通过本接口重置认证凭据。 |
| `PUT /api/v1/iam/users/{userId}/roles` | `ReplaceUserRolesRequest` | `ApiResponse<UserRoleBindingVO>` | 以完整角色集合替换绑定；需校验操作者不可越权授予角色。 |
| `PATCH /api/v1/iam/users/{userId}/status` | `ChangeUserStatusRequest` | `ApiResponse<UserStatusVO>` | 启用、停用或锁定用户；不得停用最后一个可用平台超级管理员。 |
| `DELETE /api/v1/iam/users/{userId}` | 路径参数 `userId` + `version` | `ApiResponse<Void>` | 逻辑删除；保留审计与历史业务关联，禁止物理删除。 |
| `GET /api/v1/iam/roles/options` | 可选范围参数 | `ApiResponse<List<RoleOptionVO>>` | 仅返回当前操作者可授予的角色，供受控下拉选择。 |

## 3. 关键请求与响应字段

| 模型/字段 | 类型 | 中文说明 | 约束 |
| --- | --- | --- | --- |
| `CreateUserRequest.username` | string | 管理用户登录名 | 必填；租户范围内唯一；禁止手机号、身份证等敏感字段作默认用户名。 |
| `CreateUserRequest.displayName` | string | 后台展示姓名 | 必填；按中文姓名/组织展示规则脱敏。 |
| `CreateUserRequest.roleIds` | array<number> | 初始绑定角色标识集合 | 至少一个；全部必须在操作者可授予范围内。 |
| `UpdateUserRequest.version` | number | 乐观锁版本 | 必填；冲突返回 `VERSION_CONFLICT`。 |
| `ReplaceUserRolesRequest.roleIds` | array<number> | 替换后的完整角色集合 | 必填；空集合仅在明确允许无角色保留账号时使用。 |
| `ChangeUserStatusRequest.status` | string | 目标账户状态 | 枚举和状态迁移以 IAM 状态机为准。 |
| `UserListVO.roleNames` | array<string> | 可见角色名称摘要 | 只返回操作者有权查看的角色信息。 |
| `UserDetailVO.version` | number | 用户当前并发版本 | 前端编辑/删除时原样回传。 |
| `UserDetailVO.roles` | array | 角色绑定明细 | 不返回角色内部策略、密钥或不属于当前范围的数据。 |

## 4. 后端、前端与测试落地

- 后端目录固定为 `module/iam/{controller,dto,vo,service,mapper,entity,convert,exception}`；角色替换、状态迁移和越权授予校验由 `service` 编排，复杂规则分别进入 `validator`、`statemachine` 或 `policy`，Controller 不得直连 Mapper。
- 前端目录固定为业务模块组织，例如 `views/iam/user/`、`api/iam/user.ts`、`stores/iam-user.ts`；列表、编辑、绑定角色、停用和删除均处理加载、空、失败、无权限、版本冲突与关闭态。
- 测试按“查询数据范围、创建幂等、用户名重复、角色越权、乐观锁冲突、最后管理员保护、逻辑删除不可见、审计字段完整”逐项覆盖；每个测试类、方法、夹具和关键 Given/When/Then 代码块写中文说明。

## 修订记录

| 时间 | 版本 | 修订原因 |
| --- | --- | --- |
| 2026-08-14 | v1.0 | 建立用户与角色的统一资源接口边界，防止管理用户 IAM 与 M1 个人用户授权混用。 |
