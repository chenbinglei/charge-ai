package com.elink.evco.platform.iam.service;

import com.elink.evco.platform.common.cache.RedisKeys;
import com.elink.evco.platform.common.config.AppProperties;
import com.elink.evco.platform.iam.entity.IamUser;
import com.elink.evco.platform.iam.mapper.IamPermissionMapper;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 权限读取服务：角色 → 权限并集经 Redis 缓存（TTL 与 access_token 对齐），
 * 角色变更或登出时显式驱逐；平台超级管理员返回全量启用权限码。
 *
 * <p>Redis 故障时降级直查数据库（鉴权可用性优先），并记录受控告警日志。
 */
@Service
public class PermissionService {

    /** 受控日志；Redis 故障降级记录告警。 */
    private static final Logger LOG = LoggerFactory.getLogger(PermissionService.class);

    /** 权限主数据访问。 */
    private final IamPermissionMapper permissionMapper;

    /** Redis 客户端。 */
    private final StringRedisTemplate redis;

    /** Redis 键规则。 */
    private final RedisKeys keys;

    /** access_token 有效期（分钟）；权限缓存 TTL 与其对齐。 */
    private final long accessTokenTtlMinutes;

    /**
     * 构造权限服务。
     *
     * @param permissionMapper 权限 Mapper。
     * @param redis Redis 客户端。
     * @param keys Redis 键规则。
     * @param appProperties 平台业务配置。
     */
    public PermissionService(
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
     * @param user 目标用户（含 userType）。
     * @return 权限码集合；无权限时为空集合。
     */
    public Set<String> effectivePermissions(IamUser user) {
        if (IamUser.TYPE_PLATFORM_SUPER.equals(user.getUserType())) {
            return new HashSet<>(permissionMapper.selectAllActiveCodes());
        }
        return new HashSet<>(loadAndCache(user.getId()));
    }

    /**
     * 驱逐用户权限缓存；角色替换、登出、撤销、停用时调用。
     *
     * @param userId 用户 ID。
     */
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
            Set<String> cached = redis.opsForSet().members(key);
            if (cached != null && !cached.isEmpty()) {
                return List.copyOf(cached);
            }
            List<String> codes = permissionMapper.selectActiveCodesByUserId(userId);
            if (!codes.isEmpty()) {
                redis.opsForSet().add(key, codes.toArray(String[]::new));
                redis.expire(key, Duration.ofMinutes(accessTokenTtlMinutes));
            }
            return codes;
        } catch (RuntimeException ex) {
            LOG.warn("权限缓存读取失败，降级数据库直查 userId={}", userId, ex);
            return permissionMapper.selectActiveCodesByUserId(userId);
        }
    }
}
