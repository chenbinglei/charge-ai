package com.elink.evco.platform.iam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.elink.evco.kernel.api.PageResponse;
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
import com.elink.evco.platform.iam.service.AuditService;
import com.elink.evco.platform.iam.service.IamRoleService;
import com.elink.evco.platform.iam.service.IamUserService;
import com.elink.evco.platform.iam.service.PermissionService;
import com.elink.evco.platform.iam.service.SessionService;
import com.elink.evco.platform.iam.vo.RoleOptionVO;
import com.elink.evco.platform.iam.vo.UserDetailVO;
import com.elink.evco.platform.iam.vo.UserListVO;
import com.elink.evco.platform.iam.vo.UserRoleBindingVO;
import com.elink.evco.platform.iam.vo.UserStatusVO;
import com.elink.evco.web.config.AppProperties;
import com.elink.evco.web.config.DeploymentMode;
import com.elink.evco.web.error.BusinessException;
import com.elink.evco.web.error.PlatformErrorCode;
import com.elink.evco.web.security.AuthContext;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** IAM 管理用户服务实现：租户隔离查询、随机初始密码、乐观锁编辑/删除/角色替换、 状态机迁移与最后平台超管保护。 */
@Service
public class IamUserServiceImpl implements IamUserService {

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
     * 构造用户服务实现。
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
    public IamUserServiceImpl(
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

    @Override
    public PageResponse<UserListVO> pageUsers(
            AuthContext operator,
            String username,
            String displayName,
            String status,
            Long roleId,
            int pageNo,
            int pageSize) {
        // 1. 组装租户隔离基础条件：仅本租户 + 未删除，按创建时间倒序。
        LambdaQueryWrapper<IamUser> wrapper =
                new LambdaQueryWrapper<IamUser>()
                        .eq(IamUser::getTenantId, operator.tenantId())
                        .isNull(IamUser::getDeletedAt)
                        .orderByDesc(IamUser::getCreatedAt);
        // 2. 叠加可选过滤：用户名/姓名模糊、状态精确。
        if (username != null && !username.isBlank()) {
            wrapper.like(IamUser::getUsername, username.trim());
        }
        if (displayName != null && !displayName.isBlank()) {
            wrapper.like(IamUser::getDisplayName, displayName.trim());
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(IamUser::getStatus, status.trim());
        }
        // 3. 角色过滤：先查绑定关系反推用户 ID 集合，无绑定直接返回空页。
        if (roleId != null) {
            List<Long> userIds =
                    userRoleMapper
                            .selectList(
                                    new LambdaQueryWrapper<IamUserRole>()
                                            .eq(IamUserRole::getRoleId, roleId))
                            .stream()
                            .map(IamUserRole::getUserId)
                            .distinct()
                            .toList();
            if (userIds.isEmpty()) {
                return new PageResponse<>(List.of(), pageNo, pageSize, 0);
            }
            wrapper.in(IamUser::getId, userIds);
        }
        // 4. 分页查询并批量补齐角色名，避免逐行回查。
        Page<IamUser> page = userMapper.selectPage(Page.of(pageNo, pageSize), wrapper);
        Map<Long, List<String>> roleNamesByUser =
                roleNamesByUserIds(page.getRecords().stream().map(IamUser::getId).toList());
        List<UserListVO> items =
                page.getRecords().stream()
                        .map(
                                user ->
                                        new UserListVO(
                                                String.valueOf(user.getId()),
                                                user.getUsername(),
                                                user.getDisplayName(),
                                                user.getStatus(),
                                                roleNamesByUser.getOrDefault(
                                                        user.getId(), List.of()),
                                                String.valueOf(user.getTenantId()),
                                                user.getCreatedAt()))
                        .toList();
        return new PageResponse<>(items, pageNo, pageSize, page.getTotal());
    }

    @Override
    public UserDetailVO getUser(AuthContext operator, Long userId) {
        // 1. 加载本租户可见用户（跨租户/不存在统一 IAM_USER_NOT_FOUND）。
        IamUser user = loadVisible(operator, userId);
        // 2. 组装详情（含绑定角色选项；initialPassword 仅创建时返回）。
        return toDetailVO(user, roleOptions(user.getId()), null);
    }

    @Override
    @Transactional
    public UserDetailVO createUser(AuthContext operator, CreateUserRequest request) {
        // 1. 部署模式门禁：IOT 协同模式用户域只读。
        requireStandaloneMode();
        // 2. 租户归属校验：请求租户若显式指定必须与操作者一致，禁止跨租户开户。
        Long tenantId = operator.tenantId();
        if (request.tenantId() != null && !request.tenantId().isBlank()) {
            Long requested = Long.parseLong(request.tenantId().trim());
            if (!requested.equals(tenantId)) {
                throw new BusinessException(PlatformErrorCode.FORBIDDEN, "不可跨租户创建用户");
            }
        }
        // 3. 角色可授予范围校验（存在、未删除、同租户）。
        List<Long> roleIds = parseIds(request.roleIds(), "roleIds");
        List<IamRole> roles = validateGrantableRoles(roleIds, tenantId);
        // 4. 用户名唯一性预检（同租户 + 未删除范围）。
        Boolean duplicated =
                userMapper.exists(
                        new LambdaQueryWrapper<IamUser>()
                                .eq(IamUser::getTenantId, tenantId)
                                .eq(IamUser::getUsername, request.username().trim())
                                .isNull(IamUser::getDeletedAt));
        if (Boolean.TRUE.equals(duplicated)) {
            throw new BusinessException(PlatformErrorCode.IAM_USERNAME_DUPLICATE);
        }
        // 5. 落库：BCrypt 初始密码、平台来源、激活态 + 首登强制改密。
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
        // 6. 绑定初始角色并审计留痕。
        bindRoles(user.getId(), roleIds);
        auditService.record(
                operator.userId(),
                tenantId,
                IamAuditLog.ACTION_CREATE,
                "iam_user",
                user.getId(),
                "新增管理用户 " + user.getUsername() + "，初始角色 " + roleIds.size() + " 个",
                null);
        // 7. 一次性返回初始密码（仅此响应可见）。
        return toDetailVO(user, roleServiceOptions(roles), initialPassword);
    }

    @Override
    @Transactional
    public UserDetailVO updateUser(AuthContext operator, Long userId, UpdateUserRequest request) {
        // 1. 部署模式门禁 + 租户可见性校验。
        requireStandaloneMode();
        IamUser user = loadVisible(operator, userId);
        // 2. 乐观锁条件更新：携带 version，冲突时 updateById 返回 0。
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
        // 3. 审计留痕并回读最新详情。
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

    @Override
    @Transactional
    public void deleteUser(AuthContext operator, Long userId, Integer version) {
        // 1. 部署模式门禁 + 租户可见性校验。
        requireStandaloneMode();
        IamUser user = loadVisible(operator, userId);
        // 2. 最后一个可用平台超管保护。
        protectLastPlatformSuper(user, "删除");
        // 3. 乐观锁条件逻辑删除：deleted_at 置值、version+1。
        int updated =
                userMapper.update(
                        null,
                        new LambdaUpdateWrapper<IamUser>()
                                .eq(IamUser::getId, user.getId())
                                .eq(IamUser::getVersion, version)
                                .isNull(IamUser::getDeletedAt)
                                .set(IamUser::getDeletedAt, LocalDateTime.now())
                                .set(IamUser::getVersion, version + 1)
                                .setSql("updated_at = updated_at"));
        if (updated == 0) {
            throw new BusinessException(PlatformErrorCode.VERSION_CONFLICT);
        }
        // 4. 级联清理：解绑角色、撤销全部会话、驱逐权限缓存。
        userRoleMapper.delete(
                new LambdaQueryWrapper<IamUserRole>().eq(IamUserRole::getUserId, userId));
        sessionService.revokeByUser(userId);
        permissionService.evict(userId);
        // 5. 审计留痕。
        auditService.record(
                operator.userId(),
                operator.tenantId(),
                IamAuditLog.ACTION_DELETE,
                "iam_user",
                user.getId(),
                "逻辑删除管理用户 " + user.getUsername(),
                null);
    }

    @Override
    @Transactional
    public UserRoleBindingVO replaceRoles(
            AuthContext operator, Long userId, ReplaceUserRolesRequest request) {
        // 0. 部署模式门禁 + 租户可见性校验。
        requireStandaloneMode();
        IamUser user = loadVisible(operator, userId);
        // 1. 解析目标角色集合并校验可授予范围（越权返回 IAM_ROLE_OUT_OF_SCOPE）。
        List<Long> roleIds =
                request.roleIds().isEmpty() ? List.of() : parseIds(request.roleIds(), "roleIds");
        List<IamRole> roles = validateGrantableRoles(roleIds, user.getTenantId());
        // 2. 乐观锁校验并自增版本：权限变更必须有并发冲突信号，
        //    条件更新失败说明期间发生并发修改，返回 VERSION_CONFLICT。
        LocalDateTime now = LocalDateTime.now();
        int bumped =
                userMapper.update(
                        null,
                        new LambdaUpdateWrapper<IamUser>()
                                .eq(IamUser::getId, userId)
                                .eq(IamUser::getVersion, request.version())
                                .set(IamUser::getVersion, request.version() + 1)
                                .set(IamUser::getUpdatedAt, now));
        if (bumped == 0) {
            throw new BusinessException(PlatformErrorCode.VERSION_CONFLICT);
        }
        // 3. 全量替换绑定：先清空旧绑定再写入新集合，同事务内保证原子性。
        userRoleMapper.delete(
                new LambdaQueryWrapper<IamUserRole>().eq(IamUserRole::getUserId, userId));
        bindRoles(userId, roleIds);
        // 4. 权限即时生效：清除该用户权限缓存，避免旧权限码残留至令牌过期。
        permissionService.evict(userId);
        // 5. 审计留痕：记录操作者、目标与替换后的角色数量。
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

    @Override
    @Transactional
    public UserStatusVO changeStatus(
            AuthContext operator, Long userId, ChangeUserStatusRequest request) {
        // 1. 部署模式门禁 + 租户可见性校验 + 状态机合法性校验。
        requireStandaloneMode();
        IamUser user = loadVisible(operator, userId);
        String target = request.status();
        ensureTransitionAllowed(user.getStatus(), target);
        // 2. 锁定/停用场景触发最后平台超管保护。
        if (IamUser.STATUS_LOCKED.equals(target) || IamUser.STATUS_DISABLED.equals(target)) {
            protectLastPlatformSuper(user, target.equals(IamUser.STATUS_LOCKED) ? "锁定" : "停用");
        }
        // 3. 乐观锁条件更新：按目标状态附加字段（锁定写 locked_until；激活清空锁定与失败计数）。
        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<IamUser> wrapper =
                new LambdaUpdateWrapper<IamUser>()
                        .eq(IamUser::getId, userId)
                        .eq(IamUser::getVersion, user.getVersion())
                        .set(IamUser::getStatus, target)
                        .set(IamUser::getUpdatedAt, now);
        if (IamUser.STATUS_LOCKED.equals(target)) {
            wrapper.set(
                    IamUser::getLockedUntil,
                    now.plusMinutes(appProperties.getAuth().getLockout().getLockMinutes()));
        } else if (IamUser.STATUS_ACTIVE.equals(target)) {
            wrapper.set(IamUser::getLockedUntil, null)
                    .set(IamUser::getFailCount, 0)
                    .set(IamUser::getFailWindowStart, null);
        }
        int updated = userMapper.update(null, wrapper);
        if (updated == 0) {
            throw new BusinessException(PlatformErrorCode.VERSION_CONFLICT);
        }
        // 4. 锁定/停用后立即撤销会话并驱逐权限缓存，强制下线即时生效。
        if (IamUser.STATUS_LOCKED.equals(target) || IamUser.STATUS_DISABLED.equals(target)) {
            sessionService.revokeByUser(userId);
            permissionService.evict(userId);
        }
        // 5. 审计留痕：记录状态迁移轨迹。
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
                        || (IamUser.STATUS_ACTIVE.equals(current)
                                && IamUser.STATUS_DISABLED.equals(target))
                        || (IamUser.STATUS_LOCKED.equals(current)
                                && IamUser.STATUS_ACTIVE.equals(target))
                        || (IamUser.STATUS_LOCKED.equals(current)
                                && IamUser.STATUS_DISABLED.equals(target))
                        || (IamUser.STATUS_DISABLED.equals(current)
                                && IamUser.STATUS_ACTIVE.equals(target));
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
            throw new BusinessException(
                    PlatformErrorCode.IAM_LAST_ADMIN_PROTECTED, "不得" + action + "最后一个可用平台超级管理员");
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
        Set<Long> found = roles.stream().map(IamRole::getId).collect(Collectors.toSet());
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
        Set<Long> roleIds =
                bindings.stream().map(IamUserRole::getRoleId).collect(Collectors.toSet());
        Map<Long, String> roleNames =
                roleMapper
                        .selectList(new LambdaQueryWrapper<IamRole>().in(IamRole::getId, roleIds))
                        .stream()
                        .collect(Collectors.toMap(IamRole::getId, IamRole::getName));
        Map<Long, List<String>> grouped = new HashMap<>();
        bindings.forEach(
                binding ->
                        grouped.computeIfAbsent(binding.getUserId(), key -> new ArrayList<>())
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
                                                        && role.getTenantId()
                                                                == IamRoleService.SYSTEM_TENANT_ID
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
                                                        && role.getTenantId()
                                                                == IamRoleService.SYSTEM_TENANT_ID
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
    private UserDetailVO toDetailVO(
            IamUser user, List<RoleOptionVO> roles, String initialPassword) {
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
                throw new BusinessException(
                        PlatformErrorCode.VALIDATION_ERROR, fieldName + " 格式不合法");
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
            password.append(
                    INITIAL_PASSWORD_CHARSET[random.nextInt(INITIAL_PASSWORD_CHARSET.length)]);
        }
        return password.toString();
    }

    /** 断言独立部署模式；IOT 协同模式下用户/角色域只读。 */
    private void requireStandaloneMode() {
        if (appProperties.getDeploymentMode() == DeploymentMode.IOT_COLLABORATIVE) {
            throw new BusinessException(PlatformErrorCode.DEPLOYMENT_MODE_READONLY);
        }
    }
}
