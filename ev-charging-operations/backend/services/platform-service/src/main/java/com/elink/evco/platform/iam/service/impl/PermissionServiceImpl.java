package com.elink.evco.platform.iam.service.impl;

import com.elink.evco.platform.iam.cache.RedisKeys;
import com.elink.evco.platform.iam.entity.IamUser;
import com.elink.evco.platform.iam.mapper.IamPermissionMapper;
import com.elink.evco.platform.iam.service.PermissionService;
import com.elink.evco.web.config.AppProperties;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/** 权限读取实现：超管全量、缓存优先、Redis 故障降级数据库直查。 */
@Service
public class PermissionServiceImpl implements PermissionService {

    /** 受控日志；Redis 故障降级记录告警。 */
    private static final Logger LOG = LoggerFactory.getLogger(PermissionServiceImpl.class);

    /** 权限主数据访问。 */
    private final IamPermissionMapper permissionMapper;

    /** Redis 客户端。 */
    private final StringRedisTemplate redis;

    /** Redis 键规则。 */
    private final RedisKeys keys;

    /** access_token 有效期（分钟）；权限缓存 TTL 与其对齐。 */
    private final long accessTokenTtlMinutes;

    /**
     * 构造权限实现。
     *
     * @param permissionMapper 权限 Mapper。
     * @param redis Redis 客户端。
     * @param keys Redis 键规则。
     * @param appProperties 平台业务配置。
     */
    public PermissionServiceImpl(
            IamPermissionMapper permissionMapper,
            StringRedisTemplate redis,
            RedisKeys keys,
            AppProperties appProperties) {
        this.permissionMapper = permissionMapper;
        this.redis = redis;
        this.keys = keys;
        this.accessTokenTtlMinutes = appProperties.getAuth().getAccessTokenTtlMinutes();
    }

    /**
     * 读取用户有效权限码集合；超管返回全量，其余走缓存 + 角色并集。
     *
     * <p>步骤：超管直查全量启用权限 → 普通用户缓存优先读取。
     *
     * @param userId 目标用户 ID。
     * @param userType 目标用户类型。
     * @return 权限码集合；无权限时为空集合。
     */
    @Override
    public Set<String> effectivePermissions(Long userId, String userType) {
        // 1. 平台超管不判角色，直接全量启用权限。
        if (IamUser.TYPE_PLATFORM_SUPER.equals(userType)) {
            return new HashSet<>(permissionMapper.selectAllActiveCodes());
        }
        // 2. 普通用户：缓存优先 + 角色权限并集。
        return new HashSet<>(loadAndCache(userId));
    }

    /**
     * 读取用户有效权限码集合（实体入参重载）。
     *
     * @param user 目标用户（含 userType）。
     * @return 权限码集合；无权限时为空集合。
     */
    @Override
    public Set<String> effectivePermissions(IamUser user) {
        return effectivePermissions(user.getId(), user.getUserType());
    }

    /**
     * 驱逐用户权限缓存；角色替换、登出、撤销、停用时调用。
     *
     * <p>驱逐失败不阻断业务，等待 TTL 过期兜底。
     *
     * @param userId 用户 ID。
     */
    @Override
    public void evict(Long userId) {
        try {
            redis.delete(keys.permissions(userId));
        } catch (RuntimeException ex) {
            LOG.warn("权限缓存驱逐失败 userId={}，等待 TTL 过期兜底", userId, ex);
        }
    }

    /**
     * 缓存优先读取权限码；Redis 不可用时降级数据库直查。
     *
     * @param userId 用户 ID。
     * @return 权限码列表。
     */
    private List<String> loadAndCache(Long userId) {
        String key = keys.permissions(userId);
        try {
            // 1. 缓存命中直接返回。
            Set<String> cached = redis.opsForSet().members(key);
            if (cached != null && !cached.isEmpty()) {
                return List.copyOf(cached);
            }
            // 2. 未命中查库并回填缓存（TTL 与 access_token 对齐）。
            List<String> codes = permissionMapper.selectActiveCodesByUserId(userId);
            if (!codes.isEmpty()) {
                redis.opsForSet().add(key, codes.toArray(String[]::new));
                redis.expire(key, Duration.ofMinutes(accessTokenTtlMinutes));
            }
            return codes;
        } catch (RuntimeException ex) {
            // 3. Redis 故障降级直查数据库，鉴权可用性优先。
            LOG.warn("权限缓存读取失败，降级数据库直查 userId={}", userId, ex);
            return permissionMapper.selectActiveCodesByUserId(userId);
        }
    }
}
