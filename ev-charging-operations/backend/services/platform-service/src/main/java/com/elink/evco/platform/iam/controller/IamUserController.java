package com.elink.evco.platform.iam.controller;

import com.elink.evco.kernel.api.ApiResponse;
import com.elink.evco.kernel.api.PageResponse;
import com.elink.evco.platform.iam.dto.ChangeUserStatusRequest;
import com.elink.evco.platform.iam.dto.CreateUserRequest;
import com.elink.evco.platform.iam.dto.ReplaceUserRolesRequest;
import com.elink.evco.platform.iam.dto.UpdateUserRequest;
import com.elink.evco.platform.iam.service.IamUserService;
import com.elink.evco.platform.iam.service.IdempotencyService;
import com.elink.evco.platform.iam.vo.UserDetailVO;
import com.elink.evco.platform.iam.vo.UserListVO;
import com.elink.evco.platform.iam.vo.UserRoleBindingVO;
import com.elink.evco.platform.iam.vo.UserStatusVO;
import com.elink.evco.web.security.AuthContextHolder;
import com.elink.evco.web.security.HasPermission;
import com.elink.evco.web.trace.TraceIdHolder;
import com.elink.evco.web.util.Ids;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * IAM 管理用户端点：分页查询、详情、创建（幂等 + 初始密码一次性返回）、 编辑（乐观锁）、逻辑删除、角色集合替换与状态迁移。
 *
 * <p>权限模型：GET 校验 iam_user:read，写方法校验 iam_user:write 且标记 IOT 维护数据域（iotManaged=true，IOT 协同模式下返回
 * DEPLOYMENT_MODE_READONLY）。
 */
@RestController
@RequestMapping("/api/v1/iam/users")
public class IamUserController {

    /** 用户创建幂等作用域。 */
    private static final String CREATE_IDEMPOTENCY_SCOPE = "iam-user-create";

    /** 用户服务。 */
    private final IamUserService userService;

    /** 幂等服务。 */
    private final IdempotencyService idempotencyService;

    /**
     * 构造用户端点。
     *
     * @param userService 用户服务。
     * @param idempotencyService 幂等服务。
     */
    public IamUserController(IamUserService userService, IdempotencyService idempotencyService) {
        this.userService = userService;
        this.idempotencyService = idempotencyService;
    }

    /**
     * 分页查询管理用户；按操作者租户隔离。
     *
     * @param username 登录名模糊过滤；可空。
     * @param displayName 姓名模糊过滤；可空。
     * @param status 状态精确过滤；可空。
     * @param roleId 角色精确过滤；可空。
     * @param pageNo 页码（从 1 开始）。
     * @param pageSize 页大小（1-100）。
     * @return 分页用户列表。
     */
    @GetMapping
    @HasPermission("iam_user:read")
    public ApiResponse<PageResponse<UserListVO>> pageUsers(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String displayName,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String roleId,
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "20") int pageSize) {
        PageResponse<UserListVO> data =
                userService.pageUsers(
                        AuthContextHolder.require(),
                        username,
                        displayName,
                        status,
                        roleId == null || roleId.isBlank() ? null : Ids.parse(roleId, "roleId"),
                        pageNo,
                        pageSize);
        return ApiResponse.success(data, TraceIdHolder.current().value());
    }

    /**
     * 新增管理用户并绑定初始角色；支持 X-Idempotency-Key 重放首个成功响应。
     *
     * @param idempotencyKey 幂等键；可空表示跳过幂等。
     * @param request 创建请求。
     * @return 用户详情（含一次性初始密码）。
     */
    @PostMapping
    @HasPermission(value = "iam_user:write", iotManaged = true)
    public ApiResponse<UserDetailVO> createUser(
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreateUserRequest request) {
        return idempotencyService.execute(
                CREATE_IDEMPOTENCY_SCOPE,
                idempotencyKey,
                request,
                () -> userService.createUser(AuthContextHolder.require(), request));
    }

    /**
     * 查询单一可见用户详情。
     *
     * @param userId 用户 ID（字符串形式雪花 ID）。
     * @return 用户详情（不含认证材料）。
     */
    @GetMapping("/{userId}")
    @HasPermission("iam_user:read")
    public ApiResponse<UserDetailVO> getUser(@PathVariable String userId) {
        UserDetailVO data =
                userService.getUser(AuthContextHolder.require(), Ids.parse(userId, "userId"));
        return ApiResponse.success(data, TraceIdHolder.current().value());
    }

    /**
     * 编辑管理用户资料；乐观锁版本冲突返回 VERSION_CONFLICT。
     *
     * @param userId 用户 ID。
     * @param request 编辑请求（含版本）。
     * @return 更新后的用户详情。
     */
    @PutMapping("/{userId}")
    @HasPermission(value = "iam_user:write", iotManaged = true)
    public ApiResponse<UserDetailVO> updateUser(
            @PathVariable String userId, @Valid @RequestBody UpdateUserRequest request) {
        UserDetailVO data =
                userService.updateUser(
                        AuthContextHolder.require(), Ids.parse(userId, "userId"), request);
        return ApiResponse.success(data, TraceIdHolder.current().value());
    }

    /**
     * 逻辑删除管理用户；保留审计与历史业务关联。
     *
     * @param userId 用户 ID。
     * @param version 乐观锁版本。
     * @return 空数据成功响应。
     */
    @DeleteMapping("/{userId}")
    @HasPermission(value = "iam_user:write", iotManaged = true)
    public ApiResponse<Void> deleteUser(
            @PathVariable String userId, @RequestParam Integer version) {
        userService.deleteUser(AuthContextHolder.require(), Ids.parse(userId, "userId"), version);
        return ApiResponse.success(null, TraceIdHolder.current().value());
    }

    /**
     * 以完整角色集合替换绑定；越权授予返回 IAM_ROLE_OUT_OF_SCOPE。
     *
     * @param userId 用户 ID。
     * @param request 角色替换请求。
     * @return 替换后的完整角色集合。
     */
    @PutMapping("/{userId}/roles")
    @HasPermission(value = "iam_user:write", iotManaged = true)
    public ApiResponse<UserRoleBindingVO> replaceRoles(
            @PathVariable String userId, @Valid @RequestBody ReplaceUserRolesRequest request) {
        UserRoleBindingVO data =
                userService.replaceRoles(
                        AuthContextHolder.require(), Ids.parse(userId, "userId"), request);
        return ApiResponse.success(data, TraceIdHolder.current().value());
    }

    /**
     * 启用、停用或锁定用户；状态迁移以 IAM 状态机为准。
     *
     * @param userId 用户 ID。
     * @param request 状态迁移请求。
     * @return 迁移后的用户状态。
     */
    @PatchMapping("/{userId}/status")
    @HasPermission(value = "iam_user:write", iotManaged = true)
    public ApiResponse<UserStatusVO> changeStatus(
            @PathVariable String userId, @Valid @RequestBody ChangeUserStatusRequest request) {
        UserStatusVO data =
                userService.changeStatus(
                        AuthContextHolder.require(), Ids.parse(userId, "userId"), request);
        return ApiResponse.success(data, TraceIdHolder.current().value());
    }
}
