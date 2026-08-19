package com.elink.evco.platform.iam;

import com.elink.evco.platform.common.cache.RedisKeys;
import com.elink.evco.platform.iam.entity.IamRole;
import com.elink.evco.platform.iam.entity.IamRolePermission;
import com.elink.evco.platform.iam.entity.IamUser;
import com.elink.evco.platform.iam.entity.IamUserRole;
import com.elink.evco.platform.iam.mapper.IamRoleMapper;
import com.elink.evco.platform.iam.mapper.IamRolePermissionMapper;
import com.elink.evco.platform.iam.mapper.IamUserMapper;
import com.elink.evco.platform.iam.mapper.IamUserRoleMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * IAM 集成测试公共支撑：单例 MySQL + Redis 容器（整个 JVM 生命周期共享，
 * 避免 Spring 上下文缓存指向已销毁容器）、种子用户构造与登录辅助。
 *
 * <p>种子数据直接经 Mapper 写入（测试夹具不走生产 API），权限主数据由
 * V202608240005 迁移种子提供（iam_user:read=…001、iam_user:write=…002、iam_role:read=…018）。
 * 两个子类共用同一数据库：迁移只执行一次，种子用户名必须各子类内唯一。
 */
public abstract class IamIntegrationSupport {

    /** 迁移种子中 iam_user:read 的权限 ID。 */
    static final long PERM_IAM_USER_READ = 1000000000000000001L;

    /** 迁移种子中 iam_user:write 的权限 ID。 */
    static final long PERM_IAM_USER_WRITE = 1000000000000000002L;

    /** 迁移种子中 iam_role:read 的权限 ID。 */
    static final long PERM_IAM_ROLE_READ = 1000000000000000018L;

    /** 平台级数据归属的系统租户 ID（与 IOT linkos 对齐）。 */
    static final long SYSTEM_TENANT_ID = 2069700000000000001L;

    /** 种子口令明文；测试内统一使用。 */
    static final String SEED_PASSWORD = "Seed-Passw0rd!";

    /** 单例 MySQL 容器；类加载即启动，全 JVM 共享。 */
    static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>(
                            DockerImageName.parse(
                                            "mysql:8.4.5@sha256:679e7e924f38a3cbb62a3d7df32924b83f7321a602d3f9f967c01b3df18495d6")
                                    .asCompatibleSubstituteFor("mysql"))
                    .withDatabaseName("evco_iam")
                    .withUsername("test")
                    .withPassword("test-password");

    /** 单例 Redis 容器；类加载即启动，全 JVM 共享。 */
    static final GenericContainer<?> REDIS =
            new GenericContainer<>(
                            DockerImageName.parse(
                                    "redis:7.4.2@sha256:fbdbaea47b9ae4ecc2082ecdb4e1cea81e32176ffb1dcf643d422ad07427e5d9"))
                    .withExposedPorts(6379);

    static {
        MYSQL.start();
        REDIS.start();
    }

    /** 把容器连接信息注入 Spring 环境；子类上下文统一指向单例容器。 */
    @DynamicPropertySource
    public static void registerContainers(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    /** HTTP 测试客户端。 */
    @Autowired protected TestRestTemplate rest;

    /** 用户 Mapper（种子夹具）。 */
    @Autowired protected IamUserMapper userMapper;

    /** 角色 Mapper（种子夹具）。 */
    @Autowired protected IamRoleMapper roleMapper;

    /** 用户-角色绑定 Mapper（种子夹具）。 */
    @Autowired protected IamUserRoleMapper userRoleMapper;

    /** 角色-权限绑定 Mapper（种子夹具）。 */
    @Autowired protected IamRolePermissionMapper rolePermissionMapper;

    /** 密码编码器（种子口令哈希）。 */
    @Autowired protected PasswordEncoder passwordEncoder;

    /** Redis 客户端（读取验证码内容）。 */
    @Autowired protected StringRedisTemplate redis;

    /** Redis 键规则。 */
    @Autowired protected RedisKeys keys;

    /**
     * 构造一个可用登录用户：用户 + 角色（含权限绑定）一次落库。
     *
     * @param username 登录名（租户内唯一）。
     * @param userType 用户类型（PLATFORM_SUPER/NORMAL）。
     * @param tenantId 所属租户。
     * @param permissionIds 角色绑定的权限 ID 集合。
     * @return 已落库用户实体。
     */
    protected IamUser seedUser(String username, String userType, Long tenantId, List<Long> permissionIds) {
        IamRole role = new IamRole();
        role.setTenantId(tenantId);
        role.setCode("ROLE_" + username.toUpperCase());
        role.setName(username + "的角色");
        role.setSource(IamRole.SOURCE_PLATFORM);
        roleMapper.insert(role);
        for (Long permissionId : permissionIds) {
            IamRolePermission binding = new IamRolePermission();
            binding.setRoleId(role.getId());
            binding.setPermissionId(permissionId);
            rolePermissionMapper.insert(binding);
        }
        IamUser user = new IamUser();
        user.setTenantId(tenantId);
        user.setUsername(username);
        user.setDisplayName(username + "展示名");
        user.setPasswordHash(passwordEncoder.encode(SEED_PASSWORD));
        user.setUserType(userType);
        user.setSource(IamUser.SOURCE_PLATFORM);
        user.setStatus(IamUser.STATUS_ACTIVE);
        user.setMustChangePassword(false);
        user.setFailCount(0);
        userMapper.insert(user);
        IamUserRole binding = new IamUserRole();
        binding.setUserId(user.getId());
        binding.setRoleId(role.getId());
        binding.setSource(IamUser.SOURCE_PLATFORM);
        userRoleMapper.insert(binding);
        return user;
    }

    /**
     * 获取一个验证码并从 Redis 读出其内容（测试无法 OCR 图片）。
     *
     * @return captchaId 与 captchaCode。
     */
    protected Map<String, String> fetchCaptcha() {
        ResponseEntity<Map> response = rest.getForEntity("/api/v1/auth/captcha", Map.class);
        Map<?, ?> body = response.getBody();
        Map<?, ?> data = (Map<?, ?>) body.get("data");
        String captchaId = (String) data.get("captchaId");
        String code = redis.opsForValue().get(keys.captcha(captchaId));
        return Map.of("captchaId", captchaId, "captchaCode", code);
    }

    /**
     * 以账号密码登录并返回响应体 data（令牌对等字段）。
     *
     * @param username 登录名。
     * @param password 口令。
     * @param tenantId 租户 ID；平台管理员可传 null。
     * @return 登录 data（accessToken/refreshToken/...）。
     */
    protected Map<?, ?> login(String username, String password, Long tenantId) {
        Map<String, String> captcha = fetchCaptcha();
        Map<String, Object> request = new HashMap<>();
        request.put("username", username);
        request.put("password", password);
        request.put("captchaId", captcha.get("captchaId"));
        request.put("captchaCode", captcha.get("captchaCode"));
        if (tenantId != null) {
            request.put("tenantId", String.valueOf(tenantId));
        }
        ResponseEntity<Map> response = rest.postForEntity("/api/v1/auth/login", request, Map.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("登录失败：" + response.getBody());
        }
        return (Map<?, ?>) response.getBody().get("data");
    }

    /**
     * 构造携带 Bearer 令牌的请求头。
     *
     * @param loginData 登录响应 data。
     * @return 已设置 Authorization 的请求头。
     */
    protected HttpHeaders bearer(Map<?, ?> loginData) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + loginData.get("accessToken"));
        return headers;
    }

    /**
     * 以 JSON 请求体发起带令牌的请求。
     *
     * @param method HTTP 方法。
     * @param path 请求路径。
     * @param token 登录响应 data。
     * @param body 请求体；null 表示无体。
     * @return 原始响应。
     */
    protected ResponseEntity<Map> exchange(HttpMethod method, String path, Map<?, ?> token, Object body) {
        HttpHeaders headers = bearer(token);
        return rest.exchange(path, method, new HttpEntity<>(body, headers), Map.class);
    }

    /**
     * 从统一响应外观中读取业务码。
     *
     * @param response 原始响应。
     * @return 稳定结果码字符串。
     */
    protected static String codeOf(ResponseEntity<Map> response) {
        return (String) response.getBody().get("code");
    }
}
