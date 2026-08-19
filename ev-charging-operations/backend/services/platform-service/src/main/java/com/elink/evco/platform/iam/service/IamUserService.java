package com.elink.evco.platform.iam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.elink.evco.kernel.api.PageResponse;
import com.elink.evco.platform.common.config.AppProperties;
import com.elink.evco.platform.common.config.DeploymentMode;
import com.elink.evco.platform.common.error.BusinessException;
import com.elink.evco.platform.common.error.PlatformErrorCode;
import com.elink.evco.platform.common.security.AuthContext;
import com.elink.evco.platform.iam.dto.ChangeUserStatusRequest;
import com.elink.evco.platform.iam.dto.CreateUserRequest;
import com.elink.evco.platform.iam.dto.ReplaceUserRolesRequest;
import com.elink.evco.platform.iam.dto.UpdateUserRequest;
import com.elink.evco.platform.iam.entity.IamAuditLog;
import com.elink.evco.platform.iam.entity.IamRole;
import com.elink.evco.platform.iam.entity.IamUser;
import com.elink.evco.platform.iam.entity.IamUserRole;
import com.elink.evco.platform.iam.mapper.IamRoleMapper;
import com.elink.evco.platform.iam.mapper.IamUserMapper;
import com.elink.evco.platform.iam.mapper.IamUserRoleMapper;
import com.elink.evco.platform.iam.vo.RoleOptionVO;
import com.elink.evco.platform.iam.vo.UserDetailVO;
import com.elink.evco.platform.iam.vo.UserListVO;
import com.elink.evco.platform.iam.vo.UserRoleBindingVO;
import com.elink.evco.platform.iam.vo.UserStatusVO;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * IAM 管理用户服务：分页查询、详情、创建（随机初始密码一次性返回）、编辑（乐观锁）、
 * 逻辑删除、角色集合替换与状态迁移；IOT 协同模式写操作一律 DEPLOYMENT_MODE_READONLY。
 *
 * <p>数据可见性按操作者租户隔离（IDOR 返回 IAM_USER_NOT_FOUND，不暴露资源存在）。
 */
@Service
public class IamUserService {

    /** 初始密码字符集；剔除易混淆字符。 */
    private static final char[] INITIAL_PASSWORD_CHARSET =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789".toCharArray();

    /** 初始密码长度。 */
    private static final int INITIAL_PASSWORD_LENGTH = 12;

    /** 安全随机源；初始密码生成。 */
    private final SecureRandom random = new SecureRandom();

    /** 用户数据访问。 */
    private final IamUserMapper userMapper;

    /** 角色数据访问。 */
    private final IamRoleMapper roleMapper;

    /** 用户-角色绑定数据访问。 */
    private final IamUserRoleMapper userRoleMapper;

    /** 会话与令牌管理。 */
    private final SessionService sessionService;

    /** 权限读取服务。 */
    private final PermissionService permissionService;

    /** 审计服务。 */
    private final AuditService auditService;

    /** 密码编码器（BCrypt）。 */
    private final PasswordEncoder passwordEncoder;

    /** 业务配置。 */
    private final AppProperties appProperties;

    /**
     * 构造用户服务。
     *
     * @param userMapper 用户 Mapper。
     * @param roleMapper 角色 Mapper。
     * @param userRoleMapper 用户-角色绑定 Mapper。
     * @param sessionService 会话服务。
     * @param permissionService 权限服务。
     * @param auditService 审计服务。
     * @param passwordEncoder 密码编码器。
     * @param appProperties 平台业务配置。
     */
    public IamUserService(
            IamUserMapper userMapper,
            IamRoleMapper roleMapper,
            IamUserRoleMapper userRoleMapper,
            SessionService sessionService,
            PermissionService permissionService,
            AuditService auditService,
            PasswordEncoder passwordEncoder,
            AppProperties appProperties) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.sessionService = sessionService;
        this.permissionService = permissionService;
        this.auditService = auditService;
        this.passwordEncoder = passwordEncoder;
        this.appProperties = appProperties;
    }

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
    public PageResponse<UserListVO> pageUsers(
            AuthContext operator,
            String username,
            String displayName,
            String status,
            Long roleId,
            int pageNo,
            int pageSize) {
        LambdaQueryWrapper<IamUser> wrapper =
                new LambdaQueryWrapper<IamUser>()
                        .eq(IamUser::getTenantId, operator.tenantId())
                        .isNull(IamUser::getDeletedAt)
                        .orderByDesc(IamUser::getCreatedAt);
        if (username != null && !username.isBlank()) {
            wrapper.like(IamUser::getUsername, username.trim());
        }
        if (displayName != null && !displayName.isBlank()) {
            wrapper.like(IamUser::getDisplayName, displayName.trim());
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(IamUser::getStatus, status.trim());
        }
        if (roleId != null) {
            List<Long> userIds =
                    userRoleMapper
                            .selectList(
                                    new LambdaQueryWrapper<IamUserRole>().eq(IamUserRole::getRoleId, roleId))
                            .stream()
                            .map(IamUserRole::getUserId)
                            .distinct()
                            .toList();
            if (userIds.isEmpty()) {
                return new PageResponse<>(List.of(), pageNo, pageSize, 0);
            }
            wrapper.in(IamUser::getId, userIds);
        }
        Page<IamUser> page = userMapper.selectPage(Page.of(pageNo, pageSize), wrapper);
        Map<Long, List<String>> roleNamesByUser = roleNamesByUserIds(
                page.getRecords().stream().map(IamUser::getId).toList());
        List<UserListVO> items =
                page.getRecords().stream()
                        .map(
                                user ->
                                        new UserListVO(
                                                String.valueOf(user.getId()),
                                                user.getUsername(),
                                                user.getDisplayName(),
                                                user.getStatus(),
                                                roleNamesByUser.getOrDefault(user.getId(), List.of()),
                                                String.valueOf(user.getTenantId()),
                                                user.getCreatedAt()))
                        .toList();
        return new PageResponse<>(items, pageNo, pageSize, page.getTotal());
    }

    /**
     * 查询单一可见用户详情（含绑定角色）。
     *
     * @param operator 操作者上下文。
     * @param userId 用户 ID。
     * @return 用户详情。
     */
    public UserDetailVO getUser(AuthContext operator, Long userId) {
        IamUser user = loadVisible(operator, userId);
        return toDetailVO(user, roleOptions(user.getId()), null);
    }

    /**
     * 新增管理用户并绑定初始角色；服务端生成随机初始密码，仅在响应中返回一次。
     *
     * @param operator 操作者上下文。
     * @param request 创建请求。
     * @return 用户详情（含一次性初始密码）。
     */
    @Transactional
    public UserDetailVO createUser(AuthContext operator, CreateUserRequest request) {
        requireStandaloneMode();
        Long tenantId = operator.tenantId();
        if (request.tenantId() != null && !request.tenantId().isBlank()) {
            Long requested = Long.parseLong(request.tenantId().trim());
            if (!requested.equals(tenantId)) {
                throw new BusinessException(PlatformErrorCode.FORBIDDEN, "不可跨租户创建用户");
            }
        }
        List<Long> roleIds = parseIds(request.roleIds(), "roleIds");
        List<IamRole> roles = validateGrantableRoles(roleIds, tenantId);
        Boolean duplicated =
                userMapper.exists(
                        new LambdaQueryWrapper<IamUser>()
                                .eq(IamUser::getTenantId, tenantId)
                                .eq(IamUser::getUsername, request.username().trim())
                                .isNull(IamUser::getDeletedAt));
        if (Boolean.TRUE.equals(duplicated)) {
            throw new BusinessException(PlatformErrorCode.IAM_USERNAME_DUPLICATE);
        }
        String initialPassword = randomInitialPassword();
        IamUser user = new IamUser();
        user.setTenantId(tenantId);
        user.setUsername(request.username().trim());
        user.setDisplayName(request.displayName().trim());
        user.setPasswordHash(passwordEncoder.encode(initialPassword));
        user.setUserType(IamUser.TYPE_NORMAL);
        user.setSource(IamUser.SOURCE_PLATFORM);
        user.setStatus(IamUser.STATUS_ACTIVE);
        user.setMustChangePassword(true);
        user.setFailCount(0);
        userMapper.insert(user);
        bindRoles(user.getId(), roleIds);
        auditService.record(
                operator.userId(),
                tenantId,
                IamAuditLog.ACTION_CREATE,
                "iam_user",
                user.getId(),
                "新增管理用户 " + user.getUsername() + "，初始角色 " + roleIds.size() + " 个",
                null);
        return toDetailVO(user, roleServiceOptions(roles), initialPassword);
    }

    /**
     * 编辑管理用户资料（展示姓名）；乐观锁冲突返回 VERSION_CONFLICT。
     *
     * @param operator 操作者上下文。
     * @param userId 用户 ID。
     * @param request 编辑请求。
     * @return 更新后的用户详情。
     */
    @Transactional
    public UserDetailVO updateUser(AuthContext operator, Long userId, UpdateUserRequest request) {
        requireStandaloneMode();
        IamUser user = loadVisible(operator, userId);
        IamUser patch = new IamUser();
        patch.setId(user.getId());
        patch.setVersion(request.version());
        if (request.displayName() != null && !request.displayName().isBlank()) {
            patch.setDisplayName(request.displayName().trim());
        }
        int updated = userMapper.updateById(patch);
        if (updated == 0) {
            throw new BusinessException(PlatformErrorCode.VERSION_CONFLICT);
        }
        auditService.record(
                operator.userId(),
                operator.tenantId(),
                IamAuditLog.ACTION_UPDATE,
                "iam_user",
                user.getId(),
                "编辑管理用户 " + user.getUsername() + " 资料",
                null);
        return getUser(operator, userId);
    }

    /**
     * 逻辑删除管理用户；最后管理员保护，删除后撤销全部会话。
     *
     * @param operator 操作者上下文。
     * @param userId 用户 ID。
     * @param version 乐观锁版本。
     */
    @Transactional
    public void deleteUser(AuthContext operator, Long userId, Integer version) {
        requireStandaloneMode();
        IamUser user = loadVisible(operator, userId);
        protectLastPlatformSuper(user, "删除");
        int updated =
                userMapper.update(
                        null,
                        new LambdaUpdateWrapper<IamUser>()
                                .eq(IamUser::getId, user.getId())
                                .eq(IamUser::getVersion, version)
                                .isNull(IamUser::getDeletedAt)
                                .set(IamUser::getDeletedAt, LocalDateTime.now(ZoneOffset.UTC))
                                .set(IamUser::getVersion, version + 1)
                                .setSql("updated_at = updated_at"));
        if (updated == 0) {
            throw new BusinessException(PlatformErrorCode.VERSION_CONFLICT);
        }
        userRoleMapper.delete(new LambdaQueryWrapper<IamUserRole>().eq(IamUserRole::getUserId, userId));
        sessionService.revokeByUser(userId);
        permissionService.evict(userId);
        auditService.record(
                operator.userId(),
                operator.tenantId(),
                IamAuditLog.ACTION_DELETE,
                "iam_user",
                user.getId(),
                "逻辑删除管理用户 " + user.getUsername(),
                null);
    }

    /**
     * 以完整角色集合替换绑定；越权授予返回 IAM_ROLE_OUT_OF_SCOPE。
     *
     * @param operator 操作者上下文。
     * @param userId 用户 ID。
     * @param request 替换请求。
     * @return 替换后的绑定结果。
     */
    @Transactional
    public UserRoleBindingVO replaceRoles(AuthContext operator, Long userId, ReplaceUserRolesRequest request) {
        requireStandaloneMode();
        IamUser user = loadVisible(operator, userId);
        List<Long> roleIds = request.roleIds().isEmpty()
                ? List.of()
                : parseIds(request.roleIds(), "roleIds");
        List<IamRole> roles = validateGrantableRoles(roleIds, user.getTenantId());
        userRoleMapper.delete(new LambdaQueryWrapper<IamUserRole>().eq(IamUserRole::getUserId, userId));
        bindRoles(userId, roleIds);
        permissionService.evict(userId);
        auditService.record(
                operator.userId(),
                operator.tenantId(),
                IamAuditLog.ACTION_ROLE_REPLACE,
                "iam_user",
                userId,
                "替换管理用户 " + user.getUsername() + " 角色绑定，共 " + roleIds.size() + " 个",
                null);
        return new UserRoleBindingVO(String.valueOf(userId), roleServiceOptions(roles));
    }

    /**
     * 用户状态迁移；迁移以 IAM 状态机为准，最后管理员保护。
     *
     * @param operator 操作者上下文。
     * @param userId 用户 ID。
     * @param request 状态迁移请求。
     * @return 迁移结果。
     */
    @Transactional
    public UserStatusVO changeStatus(AuthContext operator, Long userId, ChangeUserStatusRequest request) {
        requireStandaloneMode();
        IamUser user = loadVisible(operator, userId);
        String target = request.status();
        ensureTransitionAllowed(user.getStatus(), target);
        if (IamUser.STATUS_LOCKED.equals(target) || IamUser.STATUS_DISABLED.equals(target)) {
            protectLastPlatformSuper(user, target.equals(IamUser.STATUS_LOCKED) ? "锁定" : "停用");
        }
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        LambdaUpdateWrapper<IamUser> wrapper =
                new LambdaUpdateWrapper<IamUser>()
                        .eq(IamUser::getId, userId)
                        .eq(IamUser::getVersion, user.getVersion())
                        .set(IamUser::getStatus, target)
                        .set(IamUser::getUpdatedAt, now);
        if (IamUser.STATUS_LOCKED.equals(target)) {
            wrapper.set(IamUser::getLockedUntil, now.plusMinutes(appProperties.getAuth().getLockout().getLockMinutes()));
        } else if (IamUser.STATUS_ACTIVE.equals(target)) {
            wrapper.set(IamUser::getLockedUntil, null)
                    .set(IamUser::getFailCount, 0)
                    .set(IamUser::getFailWindowStart, null);
        }
        int updated = userMapper.update(null, wrapper);
        if (updated == 0) {
            throw new BusinessException(PlatformErrorCode.VERSION_CONFLICT);
        }
        if (IamUser.STATUS_LOCKED.equals(target) || IamUser.STATUS_DISABLED.equals(target)) {
            sessionService.revokeByUser(userId);
            permissionService.evict(userId);
        }
        auditService.record(
                operator.userId(),
                operator.tenantId(),
                IamAuditLog.ACTION_STATUS_CHANGE,
                "iam_user",
                userId,
                "管理用户 " + user.getUsername() + " 状态 " + user.getStatus() + " → " + target,
                null);
        return new UserStatusVO(String.valueOf(userId), target);
    }

    /**
     * 加载操作者可见用户；不存在或跨租户统一返回 IAM_USER_NOT_FOUND。
     *
     * @param operator 操作者上下文。
     * @param userId 用户 ID。
     * @return 用户实体。
     */
    private IamUser loadVisible(AuthContext operator, Long userId) {
        IamUser user =
                userMapper.selectOne(
                        new LambdaQueryWrapper<IamUser>()
                                .eq(IamUser::getId, userId)
                                .eq(IamUser::getTenantId, operator.tenantId())
                                .isNull(IamUser::getDeletedAt));
        if (user == null) {
            throw new BusinessException(PlatformErrorCode.IAM_USER_NOT_FOUND);
        }
        return user;
    }

    /**
     * 校验 IAM 用户状态机迁移合法性。
     *
     * @param current 当前状态。
     * @param target 目标状态。
     */
    private void ensureTransitionAllowed(String current, String target) {
        boolean allowed =
                (IamUser.STATUS_ACTIVE.equals(current) && IamUser.STATUS_LOCKED.equals(target))
                        || (IamUser.STATUS_ACTIVE.equals(current) && IamUser.STATUS_DISABLED.equals(target))
                        || (IamUser.STATUS_LOCKED.equals(current) && IamUser.STATUS_ACTIVE.equals(target))
                        || (IamUser.STATUS_LOCKED.equals(current) && IamUser.STATUS_DISABLED.equals(target))
                        || (IamUser.STATUS_DISABLED.equals(current) && IamUser.STATUS_ACTIVE.equals(target));
        if (!allowed) {
            throw new BusinessException(PlatformErrorCode.STATE_CONFLICT, "当前状态不允许迁移到 " + target);
        }
    }

    /**
     * 最后一个可用平台超级管理员保护。
     *
     * @param user 目标用户。
     * @param action 动作描述（用于错误信息）。
     */
    private void protectLastPlatformSuper(IamUser user, String action) {
        if (!IamUser.TYPE_PLATFORM_SUPER.equals(user.getUserType())) {
            return;
        }
        Long otherActiveSupers =
                userMapper.selectCount(
                        new LambdaQueryWrapper<IamUser>()
                                .eq(IamUser::getUserType, IamUser.TYPE_PLATFORM_SUPER)
                                .eq(IamUser::getStatus, IamUser.STATUS_ACTIVE)
                                .isNull(IamUser::getDeletedAt)
                                .ne(IamUser::getId, user.getId()));
        if (otherActiveSupers == 0) {
            throw new BusinessException(PlatformErrorCode.IAM_LAST_ADMIN_PROTECTED, "不得" + action + "最后一个可用平台超级管理员");
        }
    }

    /**
     * 校验待授予角色均在操作者可授予范围内（存在、未删除、同租户）。
     *
     * @param roleIds 角色 ID 集合。
     * @param tenantId 目标租户。
     * @return 角色实体集合。
     */
    private List<IamRole> validateGrantableRoles(List<Long> roleIds, Long tenantId) {
        if (roleIds.isEmpty()) {
            return List.of();
        }
        List<IamRole> roles =
                roleMapper.selectList(
                        new LambdaQueryWrapper<IamRole>()
                                .in(IamRole::getId, roleIds)
                                .isNull(IamRole::getDeletedAt));
        Set<Long> found =
                roles.stream().map(IamRole::getId).collect(Collectors.toSet());
        boolean allPresent = roleIds.stream().allMatch(found::contains);
        boolean sameTenant = roles.stream().allMatch(role -> tenantId.equals(role.getTenantId()));
        if (!allPresent || !sameTenant) {
            throw new BusinessException(PlatformErrorCode.IAM_ROLE_OUT_OF_SCOPE);
        }
        return roles;
    }

    /**
     * 写入用户-角色绑定。
     *
     * @param userId 用户 ID。
     * @param roleIds 角色 ID 集合。
     */
    private void bindRoles(Long userId, List<Long> roleIds) {
        for (Long roleId : roleIds) {
            IamUserRole binding = new IamUserRole();
            binding.setUserId(userId);
            binding.setRoleId(roleId);
            binding.setSource(IamUser.SOURCE_PLATFORM);
            userRoleMapper.insert(binding);
        }
    }

    /**
     * 批量查询用户角色名称映射。
     *
     * @param userIds 用户 ID 集合。
     * @return 用户 ID → 角色名称列表。
     */
    private Map<Long, List<String>> roleNamesByUserIds(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        List<IamUserRole> bindings =
                userRoleMapper.selectList(
                        new LambdaQueryWrapper<IamUserRole>().in(IamUserRole::getUserId, userIds));
        if (bindings.isEmpty()) {
            return Map.of();
        }
        Set<Long> roleIds = bindings.stream().map(IamUserRole::getRoleId).collect(Collectors.toSet());
        Map<Long, String> roleNames =
                roleMapper.selectList(new LambdaQueryWrapper<IamRole>().in(IamRole::getId, roleIds))
                        .stream()
                        .collect(Collectors.toMap(IamRole::getId, IamRole::getName));
        Map<Long, List<String>> grouped = new java.util.HashMap<>();
        bindings.forEach(
                binding -> grouped
                        .computeIfAbsent(binding.getUserId(), key -> new ArrayList<>())
                        .add(roleNames.getOrDefault(binding.getRoleId(), "未知角色")));
        return grouped;
    }

    /**
     * 查询用户绑定角色选项。
     *
     * @param userId 用户 ID。
     * @return 角色选项集合。
     */
    private List<RoleOptionVO> roleOptions(Long userId) {
        return roleMapper.selectByUserId(userId).stream()
                .map(
                        role ->
                                new RoleOptionVO(
                                        String.valueOf(role.getId()),
                                        role.getName(),
                                        role.getTenantId() != null
                                                        && role.getTenantId() == IamRoleService.SYSTEM_TENANT_ID
                                                ? "platform"
                                                : "tenant",
                                        role.getDescription()))
                .toList();
    }

    /**
     * 角色实体转选项 VO（创建/替换路径复用）。
     *
     * @param roles 角色实体集合。
     * @return 角色选项集合。
     */
    private List<RoleOptionVO> roleServiceOptions(List<IamRole> roles) {
        return roles.stream()
                .map(
                        role ->
                                new RoleOptionVO(
                                        String.valueOf(role.getId()),
                                        role.getName(),
                                        role.getTenantId() != null
                                                        && role.getTenantId() == IamRoleService.SYSTEM_TENANT_ID
                                                ? "platform"
                                                : "tenant",
                                        role.getDescription()))
                .toList();
    }

    /**
     * 组装用户详情 VO。
     *
     * @param user 用户实体。
     * @param roles 角色选项。
     * @param initialPassword 一次性初始密码；非创建场景为 null。
     * @return 用户详情 VO。
     */
    private UserDetailVO toDetailVO(IamUser user, List<RoleOptionVO> roles, String initialPassword) {
        return new UserDetailVO(
                String.valueOf(user.getId()),
                user.getUsername(),
                user.getDisplayName(),
                user.getStatus(),
                user.getVersion(),
                roles,
                String.valueOf(user.getTenantId()),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                initialPassword);
    }

    /**
     * 解析 ID 字符串集合；非法值统一 VALIDATION_ERROR。
     *
     * @param values ID 字符串集合。
     * @param fieldName 字段名。
     * @return 数值 ID 集合。
     */
    private List<Long> parseIds(List<String> values, String fieldName) {
        List<Long> ids = new ArrayList<>();
        for (String value : values) {
            if (value == null || value.isBlank()) {
                throw new BusinessException(PlatformErrorCode.VALIDATION_ERROR, fieldName + " 含空值");
            }
            try {
                ids.add(Long.parseLong(value.trim()));
            } catch (NumberFormatException ex) {
                throw new BusinessException(PlatformErrorCode.VALIDATION_ERROR, fieldName + " 格式不合法");
            }
        }
        return ids.stream().distinct().toList();
    }

    /**
     * 生成随机初始密码。
     *
     * @return 初始密码明文。
     */
    private String randomInitialPassword() {
        StringBuilder password = new StringBuilder(INITIAL_PASSWORD_LENGTH);
        for (int i = 0; i < INITIAL_PASSWORD_LENGTH; i++) {
            password.append(INITIAL_PASSWORD_CHARSET[random.nextInt(INITIAL_PASSWORD_CHARSET.length)]);
        }
        return password.toString();
    }

    /**
     * 断言独立部署模式；IOT 协同模式下用户/角色域只读。
     */
    private void requireStandaloneMode() {
        if (appProperties.getDeploymentMode() == DeploymentMode.IOT_COLLABORATIVE) {
            throw new BusinessException(PlatformErrorCode.DEPLOYMENT_MODE_READONLY);
        }
    }
}
