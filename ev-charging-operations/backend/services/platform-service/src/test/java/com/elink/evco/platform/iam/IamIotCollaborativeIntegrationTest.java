package com.elink.evco.platform.iam;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.elink.evco.platform.iam.entity.AuthSession;
import com.elink.evco.platform.iam.entity.IamRole;
import com.elink.evco.platform.iam.entity.IamUser;
import com.elink.evco.platform.iam.mapper.AuthSessionMapper;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
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
 * IOT 协同模式（IOT_COLLABORATIVE）IAM 集成测试：IOT 维护数据域写操作只读、 读接口与密码登录不受影响、SSO 一次性 ticket 推送/换会话/防重放/防探测、
 * 会话被动撤销双模式可用。
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "app.deployment-mode=IOT_COLLABORATIVE",
            "app.auth.sso.api-key=itest-sso-api-key"
        })
class IamIotCollaborativeIntegrationTest extends IamIntegrationSupport {

    /** 服务间凭证（与测试属性一致）。 */
    private static final String SSO_API_KEY = "itest-sso-api-key";

    /** SSO 目标用户的 IOT 侧用户 ID。 */
    private static final long IOT_USER_ID = 2069700000000007777L;

    /** ticket 序号；保证同 JVM 内多次推送互不冲突。 */
    private static final AtomicLong TICKET_SEQ = new AtomicLong();

    /** 管理员（读+写）登录响应。 */
    private Map<?, ?> adminToken;

    /** SSO 目标用户实体。 */
    private IamUser ssoUser;

    /** 会话数据访问（被动撤销夹具）。 */
    @Autowired private AuthSessionMapper sessionMapper;

    /** 种子账号：持写权限管理员、IOT 推送身份的 SSO 用户。 */
    @BeforeAll
    void seedAccounts() {
        seedUser(
                "iot_admin",
                IamUser.TYPE_NORMAL,
                SYSTEM_TENANT_ID,
                List.of(PERM_IAM_USER_READ, PERM_IAM_USER_WRITE, PERM_IAM_ROLE_READ));
        ssoUser =
                seedUser(
                        "iot_sso_user",
                        IamUser.TYPE_NORMAL,
                        SYSTEM_TENANT_ID,
                        List.of(PERM_IAM_USER_READ));
        // 覆写为 IOT 推送身份（source=IOT_PUSH + iot_user_id）。
        IamUser patch = new IamUser();
        patch.setId(ssoUser.getId());
        patch.setSource(IamUser.SOURCE_IOT_PUSH);
        patch.setIotUserId(IOT_USER_ID);
        userMapper.updateById(patch);
        adminToken = login("iot_admin", SEED_PASSWORD, SYSTEM_TENANT_ID);
    }

    /** IOT 协同模式下用户域写操作一律 DEPLOYMENT_MODE_READONLY。 */
    @Test
    @DisplayName("只读域：创建用户被拒")
    void createUserRejectedInCollaborativeMode() {
        Map<String, Object> body =
                Map.of(
                        "username",
                        "iot_created_01",
                        "displayName",
                        "协同模式用户",
                        "roleIds",
                        List.of(String.valueOf(roleIdOf("iot_admin"))));
        ResponseEntity<Map> response =
                exchange(HttpMethod.POST, "/api/v1/iam/users", adminToken, body);
        assertEquals(403, response.getStatusCode().value());
        assertEquals("DEPLOYMENT_MODE_READONLY", codeOf(response));
    }

    /** 读接口不受部署模式影响。 */
    @Test
    @DisplayName("只读域：列表查询正常")
    void listUsersStillWorks() {
        ResponseEntity<Map> response =
                exchange(HttpMethod.GET, "/api/v1/iam/users?username=iot_admin", adminToken, null);
        assertEquals(200, response.getStatusCode().value());
        Map<?, ?> data = (Map<?, ?>) response.getBody().get("data");
        assertEquals(1, data.get("total"));
    }

    /** 登录响应与档案均返回 IOT_COLLABORATIVE 供前端隐藏写按钮。 */
    @Test
    @DisplayName("部署模式：登录与档案标识")
    void loginAndProfileExposeCollaborativeMode() {
        assertEquals("IOT_COLLABORATIVE", adminToken.get("deploymentMode"));
        ResponseEntity<Map> profile =
                exchange(HttpMethod.GET, "/api/v1/auth/profile", adminToken, null);
        assertEquals(200, profile.getStatusCode().value());
        Map<?, ?> data = (Map<?, ?>) profile.getBody().get("data");
        assertEquals("IOT_COLLABORATIVE", data.get("deploymentMode"));
    }

    /** 缺失或错误 X-API-Key 的 ticket 推送一律拒绝。 */
    @Test
    @DisplayName("SSO：服务间凭证校验")
    void ticketPushRequiresValidApiKey() {
        Map<String, Object> push = ticketPushBody();
        ResponseEntity<Map> noKey = rest.postForEntity("/api/v1/auth/sso/tickets", push, Map.class);
        assertEquals(401, noKey.getStatusCode().value());

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-Key", "wrong-api-key");
        ResponseEntity<Map> wrongKey =
                rest.exchange(
                        "/api/v1/auth/sso/tickets",
                        HttpMethod.POST,
                        new HttpEntity<>(push, headers),
                        Map.class);
        assertEquals(401, wrongKey.getStatusCode().value());
        assertEquals("UNAUTHENTICATED", codeOf(wrongKey));
    }

    /** 正常链路：推送 ticket → 换会话成功 → 原票据重放被拒。 */
    @Test
    @DisplayName("SSO：一次性 ticket 换会话与防重放")
    void ssoTicketExchangesSessionOnce() {
        String ticket = pushTicket(SSO_API_KEY);
        Map<String, Object> loginBody = Map.of("ticket", ticket);
        ResponseEntity<Map> first =
                rest.postForEntity("/api/v1/auth/sso/login", loginBody, Map.class);
        assertEquals(200, first.getStatusCode().value());
        Map<?, ?> data = (Map<?, ?>) first.getBody().get("data");
        assertFalse(((String) data.get("accessToken")).isBlank());
        assertEquals("IOT_COLLABORATIVE", data.get("deploymentMode"));
        assertEquals(String.valueOf(ssoUser.getId()), data.get("userId"));

        ResponseEntity<Map> replay =
                rest.postForEntity("/api/v1/auth/sso/login", loginBody, Map.class);
        assertEquals(401, replay.getStatusCode().value());
        assertEquals("SSO_TICKET_INVALID", codeOf(replay));
    }

    /** 未推送过的 ticket 不区分不存在/已用/过期，防探测。 */
    @Test
    @DisplayName("SSO：未知票据防探测")
    void unknownTicketRejectedUniformly() {
        ResponseEntity<Map> response =
                rest.postForEntity(
                        "/api/v1/auth/sso/login",
                        Map.of("ticket", "unknown-ticket-" + "x".repeat(32)),
                        Map.class);
        assertEquals(401, response.getStatusCode().value());
        assertEquals("SSO_TICKET_INVALID", codeOf(response));
    }

    /** ticket 已消费后用户不可用不回滚复活，直接 SSO_USER_NOT_FOUND。 */
    @Test
    @DisplayName("SSO：未推送用户拒绝")
    void ssoLoginRejectsUnknownIotUser() {
        String ticket = pushTicket(SSO_API_KEY, 2069700000000008888L);
        ResponseEntity<Map> response =
                rest.postForEntity("/api/v1/auth/sso/login", Map.of("ticket", ticket), Map.class);
        assertEquals(403, response.getStatusCode().value());
        assertEquals("SSO_USER_NOT_FOUND", codeOf(response));
    }

    /** 会话为平台本地数据：IOT 协同模式下被动撤销仍可用。 */
    @Test
    @DisplayName("会话：协同模式下被动撤销可用")
    void revokeSessionWorksInCollaborativeMode() {
        Map<?, ?> victimToken = login("iot_sso_user", SEED_PASSWORD, SYSTEM_TENANT_ID);
        AuthSession session =
                sessionMapper.selectOne(
                        new LambdaQueryWrapper<AuthSession>()
                                .eq(AuthSession::getUserId, ssoUser.getId())
                                .eq(AuthSession::getStatus, AuthSession.STATUS_ACTIVE)
                                .orderByDesc(AuthSession::getId)
                                .last("limit 1"));
        assertNotNull(session, "受害者应存在活跃会话");

        ResponseEntity<Map> revoke =
                exchange(
                        HttpMethod.POST,
                        "/api/v1/auth/revoke",
                        adminToken,
                        Map.of("sessionId", String.valueOf(session.getId()), "reason", "测试强制下线"));
        assertEquals(200, revoke.getStatusCode().value());

        ResponseEntity<Map> after =
                exchange(HttpMethod.GET, "/api/v1/auth/profile", victimToken, null);
        assertEquals(401, after.getStatusCode().value());
    }

    /**
     * 构造合法 ticket 推送请求体。
     *
     * @return 请求体。
     */
    private Map<String, Object> ticketPushBody() {
        return ticketPushBody(IOT_USER_ID);
    }

    /**
     * 构造指定 IOT 用户的 ticket 推送请求体。
     *
     * @param iotUserId IOT 侧用户 ID。
     * @return 请求体。
     */
    private Map<String, Object> ticketPushBody(long iotUserId) {
        Map<String, Object> body = new HashMap<>();
        body.put("ticket", "t".repeat(40) + TICKET_SEQ.incrementAndGet());
        body.put("iotUserId", String.valueOf(iotUserId));
        body.put("displayName", "IOT推送用户");
        body.put("expireAt", Instant.now().plusSeconds(120).toString());
        return body;
    }

    /**
     * 以指定凭证推送 ticket 并断言成功。
     *
     * @param apiKey 服务间凭证。
     * @return ticket 原文。
     */
    private String pushTicket(String apiKey) {
        return pushTicket(apiKey, IOT_USER_ID);
    }

    /**
     * 以指定凭证与目标用户推送 ticket 并断言成功。
     *
     * @param apiKey 服务间凭证。
     * @param iotUserId IOT 侧用户 ID。
     * @return ticket 原文。
     */
    private String pushTicket(String apiKey, long iotUserId) {
        Map<String, Object> body = ticketPushBody(iotUserId);
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-Key", apiKey);
        ResponseEntity<Map> response =
                rest.exchange(
                        "/api/v1/auth/sso/tickets",
                        HttpMethod.POST,
                        new HttpEntity<>(body, headers),
                        Map.class);
        assertEquals(200, response.getStatusCode().value(), "ticket 推送应成功");
        return (String) body.get("ticket");
    }

    /**
     * 查询指定登录名对应种子角色的 ID。
     *
     * @param username 种子登录名。
     * @return 角色 ID。
     */
    private Long roleIdOf(String username) {
        return roleMapper
                .selectOne(
                        new LambdaQueryWrapper<IamRole>()
                                .eq(IamRole::getCode, "ROLE_" + username.toUpperCase()))
                .getId();
    }
}
