package com.elink.evco.platform.iam;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.elink.evco.platform.iam.entity.AuthSession;
import com.elink.evco.platform.iam.entity.IamRole;
import com.elink.evco.platform.iam.entity.IamUser;
import com.elink.evco.platform.iam.mapper.AuthSessionMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

/**
 * 独立部署模式（STANDALONE）IAM 集成测试：登录/验证码、档案与菜单推导、 用户增删改查、角色越权、租户隔离、乐观锁、状态机、令牌轮换/登出、 失败锁定与幂等重放；全部经真实 HTTP
 * + 真实 MySQL/Redis 容器验证。
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class IamStandaloneIntegrationTest extends IamIntegrationSupport {

    /** 隔离夹具租户 ID（非系统租户）。 */
    private static final long OTHER_TENANT_ID = 2069700000000000099L;

    /** 写操作者登录响应（含令牌对）。 */
    private Map<?, ?> writerToken;

    /** 只读操作者登录响应。 */
    private Map<?, ?> readerToken;

    /** 系统租户内可授予角色 ID（创建用户夹具）。 */
    private String writerRoleId;

    /** 跨租户角色 ID（越权授予夹具）。 */
    private String foreignRoleId;

    /** 隔离租户用户 ID（租户隔离夹具）。 */
    private String otherTenantUserId;

    /** 隔离租户写操作者登录响应（跨租户撤销攻击者夹具）。 */
    private Map<?, ?> otherAdminToken;

    /** 会话数据访问（撤销用例夹具）。 */
    @Autowired private AuthSessionMapper sessionMapper;

    /** 种子账号：写操作者（读+写+角色读）、只读操作者、隔离租户用户、 隔离租户写操作者（跨租户撤销攻击者）与跨租户角色。 */
    @BeforeAll
    void seedAccounts() {
        seedUser(
                "sa_writer",
                IamUser.TYPE_NORMAL,
                SYSTEM_TENANT_ID,
                List.of(PERM_IAM_USER_READ, PERM_IAM_USER_WRITE, PERM_IAM_ROLE_READ));
        seedUser("sa_reader", IamUser.TYPE_NORMAL, SYSTEM_TENANT_ID, List.of(PERM_IAM_USER_READ));
        IamUser otherTenant =
                seedUser(
                        "sa_other",
                        IamUser.TYPE_NORMAL,
                        OTHER_TENANT_ID,
                        List.of(PERM_IAM_USER_READ));
        otherTenantUserId = String.valueOf(otherTenant.getId());
        seedUser(
                "sa_other_admin",
                IamUser.TYPE_NORMAL,
                OTHER_TENANT_ID,
                List.of(PERM_IAM_USER_READ, PERM_IAM_USER_WRITE));

        IamRole writerRole =
                roleMapper.selectOne(
                        new LambdaQueryWrapper<IamRole>().eq(IamRole::getCode, "ROLE_SA_WRITER"));
        writerRoleId = String.valueOf(writerRole.getId());

        IamRole foreignRole = new IamRole();
        foreignRole.setTenantId(OTHER_TENANT_ID);
        foreignRole.setCode("ROLE_SA_FOREIGN");
        foreignRole.setName("跨租户角色");
        foreignRole.setSource(IamRole.SOURCE_PLATFORM);
        roleMapper.insert(foreignRole);
        foreignRoleId = String.valueOf(foreignRole.getId());

        writerToken = login("sa_writer", SEED_PASSWORD, SYSTEM_TENANT_ID);
        readerToken = login("sa_reader", SEED_PASSWORD, SYSTEM_TENANT_ID);
        otherAdminToken = login("sa_other_admin", SEED_PASSWORD, OTHER_TENANT_ID);
    }

    /** 验证码错误统一按认证失败拒绝，不进入口令校验。 */
    @Test
    @DisplayName("登录：验证码错误拒绝")
    void loginWithWrongCaptchaRejected() {
        Map<String, String> captcha = fetchCaptcha();
        Map<String, Object> body = new HashMap<>();
        body.put("username", "sa_writer");
        body.put("password", SEED_PASSWORD);
        body.put("captchaId", captcha.get("captchaId"));
        body.put("captchaCode", "--------");
        ResponseEntity<Map> response = rest.postForEntity("/api/v1/auth/login", body, Map.class);
        assertEquals(401, response.getStatusCode().value());
        assertEquals("UNAUTHENTICATED", codeOf(response));
    }

    /** 正确凭据登录返回令牌对与独立部署模式标识。 */
    @Test
    @DisplayName("登录：成功返回令牌对与 STANDALONE")
    void loginSuccessReturnsTokenPairAndStandaloneMode() {
        Map<?, ?> data = login("sa_writer", SEED_PASSWORD, SYSTEM_TENANT_ID);
        assertFalse(((String) data.get("accessToken")).isBlank());
        assertFalse(((String) data.get("refreshToken")).isBlank());
        assertEquals("Bearer", data.get("tokenType"));
        assertEquals("STANDALONE", data.get("deploymentMode"));
    }

    /** 档案返回权限码并集与后端推导菜单树（拥有 iam_user:read 即见平台管理员叶子）。 */
    @Test
    @DisplayName("档案：权限码与菜单推导")
    void profileExposesPermissionsAndDerivedMenus() {
        ResponseEntity<Map> response =
                exchange(HttpMethod.GET, "/api/v1/auth/profile", writerToken, null);
        assertEquals(200, response.getStatusCode().value());
        Map<?, ?> data = (Map<?, ?>) response.getBody().get("data");
        assertEquals("STANDALONE", data.get("deploymentMode"));
        List<?> permissions = (List<?>) data.get("permissions");
        assertTrue(permissions.contains("iam_user:read"));
        assertTrue(permissions.contains("iam_user:write"));
        List<?> menus = (List<?>) data.get("menus");
        assertFalse(menus.isEmpty(), "拥有 iam_user:read 应至少可见平台管理员菜单链");
    }

    /** 未携带令牌的受控请求由权限拦截器拒绝。 */
    @Test
    @DisplayName("鉴权：未携带令牌拒绝")
    void requestWithoutTokenRejected() {
        ResponseEntity<Map> response = rest.getForEntity("/api/v1/iam/users", Map.class);
        assertEquals(401, response.getStatusCode().value());
        assertEquals("UNAUTHENTICATED", codeOf(response));
    }

    /** 创建用户一次性返回初始密码；再次查询不返回，且初始密码可直接登录。 */
    @Test
    @DisplayName("用户：创建返回一次性初始密码")
    void createUserReturnsOneTimeInitialPassword() {
        Map<String, Object> body =
                Map.of(
                        "username",
                        "sa_created_01",
                        "displayName",
                        "创建用户01",
                        "roleIds",
                        List.of(writerRoleId));
        ResponseEntity<Map> response =
                exchange(HttpMethod.POST, "/api/v1/iam/users", writerToken, body);
        assertEquals(200, response.getStatusCode().value());
        Map<?, ?> data = (Map<?, ?>) response.getBody().get("data");
        assertNotNull(data.get("initialPassword"), "创建响应应返回初始密码");
        String userId = (String) data.get("id");

        ResponseEntity<Map> detail =
                exchange(HttpMethod.GET, "/api/v1/iam/users/" + userId, writerToken, null);
        assertNull(
                ((Map<?, ?>) detail.getBody().get("data")).get("initialPassword"),
                "非创建场景不得再次返回初始密码");

        Map<?, ?> createdLogin =
                login("sa_created_01", (String) data.get("initialPassword"), SYSTEM_TENANT_ID);
        assertFalse(((String) createdLogin.get("accessToken")).isBlank(), "初始密码应可登录");
    }

    /** 相同幂等键 + 相同请求体重放首个成功响应，不产生重复用户。 */
    @Test
    @DisplayName("用户：创建幂等重放")
    void createUserIdempotentReplay() {
        Map<String, Object> body =
                Map.of(
                        "username",
                        "sa_idem_01",
                        "displayName",
                        "幂等用户",
                        "roleIds",
                        List.of(writerRoleId));
        HttpHeaders headers = bearer(writerToken);
        headers.set("X-Idempotency-Key", "sa-idem-key-001");
        ResponseEntity<Map> first =
                rest.exchange(
                        "/api/v1/iam/users",
                        HttpMethod.POST,
                        new HttpEntity<>(body, headers),
                        Map.class);
        ResponseEntity<Map> second =
                rest.exchange(
                        "/api/v1/iam/users",
                        HttpMethod.POST,
                        new HttpEntity<>(body, headers),
                        Map.class);
        assertEquals(200, first.getStatusCode().value());
        assertEquals(200, second.getStatusCode().value());
        Map<?, ?> firstData = (Map<?, ?>) first.getBody().get("data");
        Map<?, ?> secondData = (Map<?, ?>) second.getBody().get("data");
        assertEquals(firstData.get("id"), secondData.get("id"), "幂等重放应返回同一用户");

        ResponseEntity<Map> page =
                exchange(
                        HttpMethod.GET, "/api/v1/iam/users?username=sa_idem_01", writerToken, null);
        Map<?, ?> pageData = (Map<?, ?>) page.getBody().get("data");
        assertEquals(1, pageData.get("total"), "幂等重放不得产生重复用户");
    }

    /** 仅持读权限的操作者调用写端点返回 FORBIDDEN。 */
    @Test
    @DisplayName("权限：只读操作者创建用户被拒")
    void createUserWithoutWritePermissionForbidden() {
        Map<String, Object> body =
                Map.of(
                        "username",
                        "sa_forbidden_01",
                        "displayName",
                        "越权用户",
                        "roleIds",
                        List.of(writerRoleId));
        ResponseEntity<Map> response =
                exchange(HttpMethod.POST, "/api/v1/iam/users", readerToken, body);
        assertEquals(403, response.getStatusCode().value());
        assertEquals("FORBIDDEN", codeOf(response));
    }

    /** 租户内重名用户创建返回 IAM_USERNAME_DUPLICATE。 */
    @Test
    @DisplayName("用户：重名创建被拒")
    void duplicateUsernameRejected() {
        Map<String, Object> body =
                Map.of(
                        "username",
                        "sa_dup_01",
                        "displayName",
                        "重复用户",
                        "roleIds",
                        List.of(writerRoleId));
        assertEquals(
                200,
                exchange(HttpMethod.POST, "/api/v1/iam/users", writerToken, body)
                        .getStatusCode()
                        .value());
        ResponseEntity<Map> second =
                exchange(HttpMethod.POST, "/api/v1/iam/users", writerToken, body);
        assertEquals(409, second.getStatusCode().value());
        assertEquals("IAM_USERNAME_DUPLICATE", codeOf(second));
    }

    /** 授予非本租户角色返回 IAM_ROLE_OUT_OF_SCOPE。 */
    @Test
    @DisplayName("用户：跨租户角色授予被拒")
    void crossTenantRoleRejected() {
        Map<String, Object> body =
                Map.of(
                        "username",
                        "sa_foreign_role_01",
                        "displayName",
                        "越权角色用户",
                        "roleIds",
                        List.of(foreignRoleId));
        ResponseEntity<Map> response =
                exchange(HttpMethod.POST, "/api/v1/iam/users", writerToken, body);
        assertEquals(403, response.getStatusCode().value());
        assertEquals("IAM_ROLE_OUT_OF_SCOPE", codeOf(response));
    }

    /** 跨租户用户不可见（IDOR 防护统一返回 IAM_USER_NOT_FOUND）。 */
    @Test
    @DisplayName("用户：跨租户用户不可见")
    void crossTenantUserInvisible() {
        ResponseEntity<Map> response =
                exchange(
                        HttpMethod.GET,
                        "/api/v1/iam/users/" + otherTenantUserId,
                        writerToken,
                        null);
        assertEquals(404, response.getStatusCode().value());
        assertEquals("IAM_USER_NOT_FOUND", codeOf(response));
    }

    /**
     * 会话撤销租户隔离（W2-D-14 IDOR）：持 iam_user:write 的他租户操作者 撤销本租户会话返回 RESOURCE_NOT_FOUND（不暴露存在性）且会话不受影响；
     * 本租户操作者撤销成功且受害者令牌立即失效。
     */
    @Test
    @DisplayName("会话：跨租户撤销被拒，本租户撤销可用")
    void revokeSessionScopedToTenant() {
        Map<?, ?> victimToken = login("sa_reader", SEED_PASSWORD, SYSTEM_TENANT_ID);
        long victimUserId = Long.parseLong((String) victimToken.get("userId"));
        AuthSession session =
                sessionMapper.selectOne(
                        new LambdaQueryWrapper<AuthSession>()
                                .eq(AuthSession::getUserId, victimUserId)
                                .eq(AuthSession::getStatus, AuthSession.STATUS_ACTIVE)
                                .orderByDesc(AuthSession::getId)
                                .last("limit 1"));
        assertNotNull(session, "受害者应存在活跃会话");

        ResponseEntity<Map> cross =
                exchange(
                        HttpMethod.POST,
                        "/api/v1/auth/revoke",
                        otherAdminToken,
                        Map.of("sessionId", String.valueOf(session.getId()), "reason", "越权尝试"));
        assertEquals(404, cross.getStatusCode().value());
        assertEquals("RESOURCE_NOT_FOUND", codeOf(cross));

        ResponseEntity<Map> stillValid =
                exchange(HttpMethod.GET, "/api/v1/auth/profile", victimToken, null);
        assertEquals(200, stillValid.getStatusCode().value(), "越权尝试不得影响会话可用性");

        ResponseEntity<Map> revoke =
                exchange(
                        HttpMethod.POST,
                        "/api/v1/auth/revoke",
                        writerToken,
                        Map.of("sessionId", String.valueOf(session.getId()), "reason", "测试强制下线"));
        assertEquals(200, revoke.getStatusCode().value());

        ResponseEntity<Map> after =
                exchange(HttpMethod.GET, "/api/v1/auth/profile", victimToken, null);
        assertEquals(401, after.getStatusCode().value(), "本租户撤销后受害者令牌应立即失效");
    }

    /** 乐观锁：过期版本编辑返回 VERSION_CONFLICT。 */
    @Test
    @DisplayName("用户：乐观锁冲突")
    void updateUserWithStaleVersionConflicts() {
        String userId = createUserViaApi("sa_version_01", "版本用户");
        Map<?, ?> detail =
                (Map<?, ?>)
                        exchange(HttpMethod.GET, "/api/v1/iam/users/" + userId, writerToken, null)
                                .getBody()
                                .get("data");
        int version = ((Number) detail.get("version")).intValue();

        ResponseEntity<Map> first =
                exchange(
                        HttpMethod.PUT,
                        "/api/v1/iam/users/" + userId,
                        writerToken,
                        Map.of("displayName", "版本用户-新名", "version", version));
        assertEquals(200, first.getStatusCode().value());

        ResponseEntity<Map> stale =
                exchange(
                        HttpMethod.PUT,
                        "/api/v1/iam/users/" + userId,
                        writerToken,
                        Map.of("displayName", "版本用户-再改名", "version", version));
        assertEquals(409, stale.getStatusCode().value());
        assertEquals("VERSION_CONFLICT", codeOf(stale));
    }

    /** 状态机：active→locked→active→disabled，非法迁移 disabled→locked 拒绝。 */
    @Test
    @DisplayName("用户：状态机迁移")
    void statusTransitionsFollowStateMachine() {
        String userId = createUserViaApi("sa_status_01", "状态用户");
        assertEquals("locked", changeStatus(userId, "locked"));
        assertEquals("active", changeStatus(userId, "active"));
        assertEquals("disabled", changeStatus(userId, "disabled"));

        ResponseEntity<Map> illegal =
                exchange(
                        HttpMethod.PATCH,
                        "/api/v1/iam/users/" + userId + "/status",
                        writerToken,
                        Map.of("status", "locked"));
        assertEquals(409, illegal.getStatusCode().value());
        assertEquals("STATE_CONFLICT", codeOf(illegal));

        assertEquals("active", changeStatus(userId, "active"));
    }

    /** 逻辑删除后用户不可见。 */
    @Test
    @DisplayName("用户：逻辑删除后不可见")
    void deleteUserSoftDeletesAndHides() {
        String userId = createUserViaApi("sa_delete_01", "删除用户");
        Map<?, ?> detail =
                (Map<?, ?>)
                        exchange(HttpMethod.GET, "/api/v1/iam/users/" + userId, writerToken, null)
                                .getBody()
                                .get("data");
        int version = ((Number) detail.get("version")).intValue();

        ResponseEntity<Map> deleted =
                exchange(
                        HttpMethod.DELETE,
                        "/api/v1/iam/users/" + userId + "?version=" + version,
                        writerToken,
                        null);
        assertEquals(200, deleted.getStatusCode().value());

        ResponseEntity<Map> after =
                exchange(HttpMethod.GET, "/api/v1/iam/users/" + userId, writerToken, null);
        assertEquals(404, after.getStatusCode().value());
        assertEquals("IAM_USER_NOT_FOUND", codeOf(after));
    }

    /** 角色选项仅返回操作者租户内角色；无 iam_role:read 的操作者被拒。 */
    @Test
    @DisplayName("角色：选项按租户过滤且需读权限")
    void roleOptionsScopedAndGuarded() {
        ResponseEntity<Map> response =
                exchange(HttpMethod.GET, "/api/v1/iam/roles/options", writerToken, null);
        assertEquals(200, response.getStatusCode().value());
        List<?> options = (List<?>) response.getBody().get("data");
        assertFalse(options.isEmpty());
        assertTrue(
                options.stream()
                        .noneMatch(option -> foreignRoleId.equals(((Map<?, ?>) option).get("id"))),
                "不得出现跨租户角色");

        ResponseEntity<Map> forbidden =
                exchange(HttpMethod.GET, "/api/v1/iam/roles/options", readerToken, null);
        assertEquals(403, forbidden.getStatusCode().value());
        assertEquals("FORBIDDEN", codeOf(forbidden));
    }

    /** 角色替换乐观锁：过期版本替换返回 VERSION_CONFLICT 且绑定不变； 正确版本替换成功后版本自增。 */
    @Test
    @DisplayName("角色：替换乐观锁冲突与成功自增")
    void replaceRolesWithStaleVersionConflicts() {
        String userId = createUserViaApi("sa_role_replace_01", "角色替换用户");
        Map<?, ?> detail =
                (Map<?, ?>)
                        exchange(HttpMethod.GET, "/api/v1/iam/users/" + userId, writerToken, null)
                                .getBody()
                                .get("data");
        int version = ((Number) detail.get("version")).intValue();

        // 第一次替换成功：版本自增。
        ResponseEntity<Map> first =
                exchange(
                        HttpMethod.PUT,
                        "/api/v1/iam/users/" + userId + "/roles",
                        writerToken,
                        Map.of("roleIds", List.of(writerRoleId), "version", version));
        assertEquals(200, first.getStatusCode().value());
        Map<?, ?> afterFirst =
                (Map<?, ?>)
                        exchange(HttpMethod.GET, "/api/v1/iam/users/" + userId, writerToken, null)
                                .getBody()
                                .get("data");
        assertEquals(version + 1, ((Number) afterFirst.get("version")).intValue(), "替换成功后版本应自增");

        // 持旧版本再替换：返回 VERSION_CONFLICT。
        ResponseEntity<Map> stale =
                exchange(
                        HttpMethod.PUT,
                        "/api/v1/iam/users/" + userId + "/roles",
                        writerToken,
                        Map.of("roleIds", List.of(), "version", version));
        assertEquals(409, stale.getStatusCode().value());
        assertEquals("VERSION_CONFLICT", codeOf(stale));

        // 冲突后绑定保持第一次替换结果（未解绑）。
        List<?> roles =
                (List<?>)
                        ((Map<?, ?>)
                                        exchange(
                                                        HttpMethod.GET,
                                                        "/api/v1/iam/users/" + userId,
                                                        writerToken,
                                                        null)
                                                .getBody()
                                                .get("data"))
                                .get("roles");
        assertFalse(roles.isEmpty(), "冲突替换不得清空既有绑定");
    }

    /** refresh_token 一次性轮换：旧令牌重复使用返回 AUTH_REFRESH_TOKEN_USED。 */
    @Test
    @DisplayName("令牌：刷新一次性轮换")
    void refreshTokenRotatesOnce() {
        Map<?, ?> data = login("sa_reader", SEED_PASSWORD, SYSTEM_TENANT_ID);
        ResponseEntity<Map> first =
                rest.postForEntity(
                        "/api/v1/auth/refresh",
                        Map.of("refreshToken", data.get("refreshToken")),
                        Map.class);
        assertEquals(200, first.getStatusCode().value());
        Map<?, ?> rotated = (Map<?, ?>) first.getBody().get("data");
        assertFalse(((String) rotated.get("accessToken")).isBlank());

        ResponseEntity<Map> replay =
                rest.postForEntity(
                        "/api/v1/auth/refresh",
                        Map.of("refreshToken", data.get("refreshToken")),
                        Map.class);
        assertEquals(401, replay.getStatusCode().value());
        assertEquals("AUTH_REFRESH_TOKEN_USED", codeOf(replay));
    }

    /** 登出撤销会话后原 access_token 立即失效。 */
    @Test
    @DisplayName("令牌：登出后会话失效")
    void logoutRevokesSession() {
        Map<?, ?> data = login("sa_reader", SEED_PASSWORD, SYSTEM_TENANT_ID);
        ResponseEntity<Map> logout =
                rest.postForEntity(
                        "/api/v1/auth/logout",
                        Map.of("refreshToken", data.get("refreshToken")),
                        Map.class);
        assertEquals(200, logout.getStatusCode().value());

        ResponseEntity<Map> profile = exchange(HttpMethod.GET, "/api/v1/auth/profile", data, null);
        assertEquals(401, profile.getStatusCode().value());
        assertEquals("AUTH_TOKEN_INVALID", codeOf(profile));
    }

    /** 连续 5 次口令失败锁定 30 分钟；锁定期内正确口令亦被拒绝。 */
    @Test
    @DisplayName("登录：五次失败锁定")
    void fivePasswordFailuresLockAccount() {
        seedUser("sa_lockme", IamUser.TYPE_NORMAL, SYSTEM_TENANT_ID, List.of(PERM_IAM_USER_READ));
        for (int i = 0; i < 5; i++) {
            ResponseEntity<Map> failure = attemptLogin("sa_lockme", "Wrong-Pass-" + i);
            assertEquals(401, failure.getStatusCode().value(), "第 " + (i + 1) + " 次失败应返回 401");
        }
        ResponseEntity<Map> locked = attemptLogin("sa_lockme", SEED_PASSWORD);
        assertEquals(401, locked.getStatusCode().value());
        String message = (String) locked.getBody().get("message");
        assertTrue(message != null && message.contains("锁定"), "锁定期内应提示账号已锁定");
    }

    /** 独立部署模式下 SSO 推送与免登录入口整体关闭。 */
    @Test
    @DisplayName("SSO：独立部署模式入口关闭")
    void ssoEntrypointClosedInStandaloneMode() {
        Map<String, Object> push = new HashMap<>();
        push.put("ticket", "s".repeat(40));
        push.put("iotUserId", "2069700000000000001");
        push.put("expireAt", java.time.Instant.now().plusSeconds(120).toString());
        ResponseEntity<Map> pushResponse =
                rest.postForEntity("/api/v1/auth/sso/tickets", push, Map.class);
        assertEquals(403, pushResponse.getStatusCode().value());
        assertEquals("FORBIDDEN", codeOf(pushResponse));

        ResponseEntity<Map> loginResponse =
                rest.postForEntity(
                        "/api/v1/auth/sso/login", Map.of("ticket", "s".repeat(40)), Map.class);
        assertEquals(403, loginResponse.getStatusCode().value());
        assertEquals("FORBIDDEN", codeOf(loginResponse));
    }

    /**
     * 经 API 创建一个可用目标用户并返回其 ID。
     *
     * @param username 登录名（须唯一）。
     * @param displayName 展示姓名。
     * @return 新用户 ID。
     */
    private String createUserViaApi(String username, String displayName) {
        ResponseEntity<Map> response =
                exchange(
                        HttpMethod.POST,
                        "/api/v1/iam/users",
                        writerToken,
                        Map.of(
                                "username",
                                username,
                                "displayName",
                                displayName,
                                "roleIds",
                                List.of(writerRoleId)));
        assertEquals(200, response.getStatusCode().value(), "夹具用户创建失败：" + response.getBody());
        return (String) ((Map<?, ?>) response.getBody().get("data")).get("id");
    }

    /**
     * 执行状态迁移并断言成功，返回迁移后状态。
     *
     * @param userId 用户 ID。
     * @param status 目标状态。
     * @return 迁移后状态。
     */
    private String changeStatus(String userId, String status) {
        ResponseEntity<Map> response =
                exchange(
                        HttpMethod.PATCH,
                        "/api/v1/iam/users/" + userId + "/status",
                        writerToken,
                        Map.of("status", status));
        assertEquals(200, response.getStatusCode().value(), "迁移到 " + status + " 应成功");
        return (String) ((Map<?, ?>) response.getBody().get("data")).get("status");
    }

    /**
     * 以新验证码尝试登录（不校验结果）。
     *
     * @param username 登录名。
     * @param password 口令。
     * @return 原始登录响应。
     */
    private ResponseEntity<Map> attemptLogin(String username, String password) {
        Map<String, String> captcha = fetchCaptcha();
        Map<String, Object> body = new HashMap<>();
        body.put("username", username);
        body.put("password", password);
        body.put("captchaId", captcha.get("captchaId"));
        body.put("captchaCode", captcha.get("captchaCode"));
        return rest.postForEntity("/api/v1/auth/login", body, Map.class);
    }
}
