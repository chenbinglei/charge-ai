package com.elink.evco.platform.iam.service;

import com.elink.evco.kernel.api.PageResponse;
import com.elink.evco.platform.iam.dto.ChangeUserStatusRequest;
import com.elink.evco.platform.iam.dto.CreateUserRequest;
import com.elink.evco.platform.iam.dto.ReplaceUserRolesRequest;
import com.elink.evco.platform.iam.dto.UpdateUserRequest;
import com.elink.evco.platform.iam.vo.UserDetailVO;
import com.elink.evco.platform.iam.vo.UserListVO;
import com.elink.evco.platform.iam.vo.UserRoleBindingVO;
import com.elink.evco.platform.iam.vo.UserStatusVO;
import com.elink.evco.web.security.AuthContext;

/**
 * IAM 管理用户服务：分页查询、详情、创建（随机初始密码一次性返回）、编辑（乐观锁）、 逻辑删除、角色集合替换（乐观锁）与状态迁移；IOT 协同模式写操作一律
 * DEPLOYMENT_MODE_READONLY。
 *
 * <p>数据可见性按操作者租户隔离（IDOR 返回 IAM_USER_NOT_FOUND，不暴露资源存在）。
 */
public interface IamUserService {

    /**
     * 分页查询管理用户；按操作者租户隔离，支持用户名/姓名/状态/角色过滤。
     *
     * @param operator 操作者上下文。
     * @param username 登录名模糊过滤；可空。
     * @param displayName 姓名模糊过滤；可空。
     * @param status 状态精确过滤；可空。
     * @param roleId 角色精确过滤；可空。
     * @param pageNo 页码（从 1 开始）。
     * @param pageSize 页大小（1-100）。
     * @return 分页用户列表。
     */
    PageResponse<UserListVO> pageUsers(
            AuthContext operator,
            String username,
            String displayName,
            String status,
            Long roleId,
            int pageNo,
            int pageSize);

    /**
     * 查询单一可见用户详情（含绑定角色）。
     *
     * @param operator 操作者上下文。
     * @param userId 用户 ID。
     * @return 用户详情。
     */
    UserDetailVO getUser(AuthContext operator, Long userId);

    /**
     * 新增管理用户并绑定初始角色；服务端生成随机初始密码，仅在响应中返回一次。
     *
     * @param operator 操作者上下文。
     * @param request 创建请求。
     * @return 用户详情（含一次性初始密码）。
     */
    UserDetailVO createUser(AuthContext operator, CreateUserRequest request);

    /**
     * 编辑管理用户资料（展示姓名）；乐观锁冲突返回 VERSION_CONFLICT。
     *
     * @param operator 操作者上下文。
     * @param userId 用户 ID。
     * @param request 编辑请求。
     * @return 更新后的用户详情。
     */
    UserDetailVO updateUser(AuthContext operator, Long userId, UpdateUserRequest request);

    /**
     * 逻辑删除管理用户；最后管理员保护，删除后撤销全部会话。
     *
     * @param operator 操作者上下文。
     * @param userId 用户 ID。
     * @param version 乐观锁版本。
     */
    void deleteUser(AuthContext operator, Long userId, Integer version);

    /**
     * 以完整角色集合替换绑定；越权授予返回 IAM_ROLE_OUT_OF_SCOPE。
     *
     * <p>携带乐观锁版本（request.version()）：条件更新失败即并发冲突，返回 VERSION_CONFLICT。
     *
     * @param operator 操作者上下文。
     * @param userId 用户 ID。
     * @param request 替换请求（含 roleIds 与乐观锁 version）。
     * @return 替换后的绑定结果。
     */
    UserRoleBindingVO replaceRoles(
            AuthContext operator, Long userId, ReplaceUserRolesRequest request);

    /**
     * 用户状态迁移；迁移以 IAM 状态机为准，最后管理员保护。
     *
     * @param operator 操作者上下文。
     * @param userId 用户 ID。
     * @param request 状态迁移请求。
     * @return 迁移结果。
     */
    UserStatusVO changeStatus(AuthContext operator, Long userId, ChangeUserStatusRequest request);
}
