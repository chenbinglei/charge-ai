package com.elink.evco.platform.iam.controller;

import com.elink.evco.kernel.api.ApiResponse;
import com.elink.evco.platform.common.security.AuthContextHolder;
import com.elink.evco.platform.common.security.HasPermission;
import com.elink.evco.platform.common.web.Ids;
import com.elink.evco.platform.common.web.TraceIdHolder;
import com.elink.evco.platform.iam.dto.LoginRequest;
import com.elink.evco.platform.iam.dto.LogoutRequest;
import com.elink.evco.platform.iam.dto.RefreshRequest;
import com.elink.evco.platform.iam.dto.RevokeRequest;
import com.elink.evco.platform.iam.dto.SsoLoginRequest;
import com.elink.evco.platform.iam.dto.SsoTicketPushRequest;
import com.elink.evco.platform.iam.service.AuthService;
import com.elink.evco.platform.iam.service.CaptchaService;
import com.elink.evco.platform.iam.service.IdempotencyService;
import com.elink.evco.platform.iam.service.SsoService;
import com.elink.evco.platform.iam.vo.CaptchaVO;
import com.elink.evco.platform.iam.vo.LoginVO;
import com.elink.evco.platform.iam.vo.ProfileVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Optional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证端点：图形验证码、账号密码登录、SSO 免登录（IOT 协同模式）、
 * 档案（权限/菜单/部署模式）、登出、刷新与被动撤销。
 *
 * <p>登录/SSO 端点为匿名入口（由 BearerAuthFilter 白名单放行）；
 * profile/revoke 需有效令牌；revoke 按 iam_user:write 两档权限校验。
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    /** 登录幂等作用域。 */
    private static final String LOGIN_IDEMPOTENCY_SCOPE = "auth-login";

    /** 服务间凭证请求头；IOT 推送 SSO ticket 携带。 */
    private static final String API_KEY_HEADER = "X-API-Key";

    /** 代理转发链路头；取第一跳作为客户端真实 IP。 */
    private static final String FORWARDED_FOR_HEADER = "X-Forwarded-For";

    /** 认证服务。 */
    private final AuthService authService;

    /** 验证码服务。 */
    private final CaptchaService captchaService;

    /** SSO 免登录服务。 */
    private final SsoService ssoService;

    /** 幂等服务。 */
    private final IdempotencyService idempotencyService;

    /** JSON 序列化器；幂等重放响应反序列化。 */
    private final ObjectMapper objectMapper;

    /**
     * 构造认证端点。
     *
     * @param authService 认证服务。
     * @param captchaService 验证码服务。
     * @param ssoService SSO 服务。
     * @param idempotencyService 幂等服务。
     * @param objectMapper JSON 序列化器。
     */
    public AuthController(
            AuthService authService,
            CaptchaService captchaService,
            SsoService ssoService,
            IdempotencyService idempotencyService,
            ObjectMapper objectMapper) {
        this.authService = authService;
        this.captchaService = captchaService;
        this.ssoService = ssoService;
        this.idempotencyService = idempotencyService;
        this.objectMapper = objectMapper;
    }

    /**
     * 获取图形验证码；Redis 存储 120 秒，登录时一次性校验。
     *
     * @return 验证码标识与 Base64 PNG 图片。
     */
    @GetMapping("/captcha")
    public ApiResponse<CaptchaVO> captcha() {
        return ApiResponse.success(captchaService.generate(), TraceIdHolder.current().value());
    }

    /**
     * 平台管理端登录；支持 X-Idempotency-Key 重放首个成功响应。
     *
     * @param idempotencyKey 幂等键；可空表示跳过幂等。
     * @param request 登录请求（含图形验证码）。
     * @param servletRequest 当前请求（提取来源 IP 与 User-Agent）。
     * @return 令牌对、角色与部署模式。
     */
    @PostMapping("/login")
    public ApiResponse<LoginVO> login(
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest) {
        Optional<ApiResponse<LoginVO>> replayed =
                replayLogin(idempotencyKey, request);
        if (replayed.isPresent()) {
            return replayed.get();
        }
        LoginVO data =
                authService.login(
                        request, clientIp(servletRequest), servletRequest.getHeader("User-Agent"));
        ApiResponse<LoginVO> response =
                ApiResponse.success(data, TraceIdHolder.current().value());
        idempotencyService.complete(LOGIN_IDEMPOTENCY_SCOPE, idempotencyKey, response);
        return response;
    }

    /**
     * IOT 推送一次性 SSO ticket（服务间接口）；仅 IOT 协同模式启用。
     *
     * @param request 推送请求。
     * @param apiKey 服务间凭证 X-API-Key。
     * @return 空数据成功响应。
     */
    @PostMapping("/sso/tickets")
    public ApiResponse<Void> pushSsoTicket(
            @Valid @RequestBody SsoTicketPushRequest request,
            @RequestHeader(value = API_KEY_HEADER, required = false) String apiKey) {
        ssoService.pushTicket(request, apiKey);
        return ApiResponse.success(null, TraceIdHolder.current().value());
    }

    /**
     * SSO 免登录：一次性 ticket 换会话；仅 IOT 协同模式启用。
     *
     * @param request SSO 登录请求。
     * @param servletRequest 当前请求。
     * @return 与密码登录一致的令牌对。
     */
    @PostMapping("/sso/login")
    public ApiResponse<LoginVO> ssoLogin(
            @Valid @RequestBody SsoLoginRequest request, HttpServletRequest servletRequest) {
        LoginVO data =
                ssoService.ssoLogin(
                        request.ticket(),
                        clientIp(servletRequest),
                        servletRequest.getHeader("User-Agent"));
        return ApiResponse.success(data, TraceIdHolder.current().value());
    }

    /**
     * 当前用户档案：权限码、部署模式与后端推导的可见菜单树。
     *
     * @return 档案响应。
     */
    @GetMapping("/profile")
    public ApiResponse<ProfileVO> profile() {
        ProfileVO data = authService.profile(AuthContextHolder.require().userId());
        return ApiResponse.success(data, TraceIdHolder.current().value());
    }

    /**
     * 主动登出；凭请求体 refresh_token 撤销会话（access_token 过期后仍可用）。
     *
     * @param request 登出请求。
     * @return 空数据成功响应。
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request.refreshToken());
        return ApiResponse.success(null, TraceIdHolder.current().value());
    }

    /**
     * 刷新 access_token；refresh_token 一次性使用并轮换新令牌对。
     *
     * @param request 刷新请求。
     * @return 新令牌对。
     */
    @PostMapping("/refresh")
    public ApiResponse<LoginVO> refresh(@Valid @RequestBody RefreshRequest request) {
        LoginVO data = authService.refresh(request.refreshToken());
        return ApiResponse.success(data, TraceIdHolder.current().value());
    }

    /**
     * 被动撤销（管理员强制下线）；会话为平台本地数据，双模式均可用。
     *
     * @param request 撤销请求。
     * @return 空数据成功响应。
     */
    @PostMapping("/revoke")
    @HasPermission("iam_user:write")
    public ApiResponse<Void> revoke(@Valid @RequestBody RevokeRequest request) {
        authService.revoke(
                Ids.parse(request.sessionId(), "sessionId"),
                request.reason(),
                AuthContextHolder.require());
        return ApiResponse.success(null, TraceIdHolder.current().value());
    }

    /**
     * 登录幂等重放：命中缓存时反序列化首个成功响应原样返回。
     *
     * @param idempotencyKey 幂等键。
     * @param request 登录请求（指纹比对）。
     * @return 命中时返回缓存响应；未命中返回空。
     */
    private Optional<ApiResponse<LoginVO>> replayLogin(String idempotencyKey, LoginRequest request) {
        Optional<String> cached =
                idempotencyService.begin(LOGIN_IDEMPOTENCY_SCOPE, idempotencyKey, request);
        if (cached.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(
                    objectMapper.readValue(cached.get(), new TypeReference<ApiResponse<LoginVO>>() {}));
        } catch (JsonProcessingException ex) {
            // 缓存内容仅由本服务写入；损坏即程序缺陷，快速失败暴露问题。
            throw new IllegalStateException("登录幂等缓存损坏", ex);
        }
    }

    /**
     * 提取客户端真实 IP；优先取代理链第一跳，防御头缺失场景。
     *
     * @param request 当前请求。
     * @return 客户端 IP。
     */
    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader(FORWARDED_FOR_HEADER);
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
