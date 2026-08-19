package com.elink.evco.platform.common.security;

import com.elink.evco.kernel.api.ApiResponse;
import com.elink.evco.platform.common.error.BusinessException;
import com.elink.evco.platform.common.error.PlatformErrorCode;
import com.elink.evco.platform.common.web.TraceIdHolder;
import com.elink.evco.platform.iam.service.SessionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Bearer 令牌过滤器：解析 JWT → 校验会话有效性（Redis 快路径 + DB 回源）→
 * 绑定认证上下文；白名单路径（验证码/登录/SSO/登出/刷新）直接放行，
 * 其余未携带令牌的请求以匿名身份进入，由权限拦截器或端点按需拒绝。
 *
 * <p>过滤器异常不在 @RestControllerAdvice 覆盖范围内，因此就地输出统一 ApiResponse 外观，
 * 保证令牌失败路径与业务失败路径外观一致。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class BearerAuthFilter extends OncePerRequestFilter {

    /** 认证头。 */
    private static final String AUTHORIZATION_HEADER = "Authorization";

    /** Bearer 方案前缀。 */
    private static final String BEARER_PREFIX = "Bearer ";

    /** 免认证路径前缀：健康检查与错误转发。 */
    private static final List<String> PREFIX_WHITELIST = List.of("/actuator", "/error");

    /**
     * 免认证的精确路径（方法 + 路径）。
     *
     * <p>登出与刷新凭请求体中的 refresh_token 自认证：access_token 已过期时仍必须可用，
     * 否则过期令牌会把刷新/登出流程一并锁死。
     */
    private static final List<AnonymousEndpoint> EXACT_WHITELIST =
            List.of(
                    new AnonymousEndpoint(HttpMethod.GET, "/api/v1/auth/captcha"),
                    new AnonymousEndpoint(HttpMethod.POST, "/api/v1/auth/login"),
                    new AnonymousEndpoint(HttpMethod.POST, "/api/v1/auth/sso/login"),
                    new AnonymousEndpoint(HttpMethod.POST, "/api/v1/auth/sso/tickets"),
                    new AnonymousEndpoint(HttpMethod.POST, "/api/v1/auth/logout"),
                    new AnonymousEndpoint(HttpMethod.POST, "/api/v1/auth/refresh"));

    /** JWT 签发器。 */
    private final JwtTokenProvider jwtTokenProvider;

    /** 会话服务。 */
    private final SessionService sessionService;

    /** JSON 序列化器；过滤器内失败响应输出。 */
    private final ObjectMapper objectMapper;

    /**
     * 构造认证过滤器。
     *
     * @param jwtTokenProvider JWT 签发器。
     * @param sessionService 会话服务。
     * @param objectMapper JSON 序列化器。
     */
    public BearerAuthFilter(
            JwtTokenProvider jwtTokenProvider,
            SessionService sessionService,
            ObjectMapper objectMapper) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.sessionService = sessionService;
        this.objectMapper = objectMapper;
    }

    /**
     * 解析令牌并绑定认证上下文；令牌存在但无效时立即拒绝（不静默降级为匿名）。
     *
     * @param request 当前请求。
     * @param response 响应。
     * @param filterChain 过滤器链。
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (isWhitelisted(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        String token = extractToken(request);
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }
        try {
            AuthContext context = jwtTokenProvider.parse(token);
            Long userId = sessionService.validateActive(context.sessionId());
            if (userId == null || !userId.equals(context.userId())) {
                throw new BusinessException(PlatformErrorCode.AUTH_TOKEN_INVALID, "会话已失效");
            }
            AuthContextHolder.set(context);
            filterChain.doFilter(request, response);
        } catch (BusinessException ex) {
            writeFailure(response, ex);
        } finally {
            AuthContextHolder.clear();
        }
    }

    /**
     * 过滤器内就地输出统一失败外观；避免落入容器默认错误页破坏响应契约。
     *
     * @param response 响应。
     * @param ex 业务异常。
     * @throws IOException 写出响应失败。
     */
    private void writeFailure(HttpServletResponse response, BusinessException ex) throws IOException {
        response.setStatus(ex.errorCode().httpStatus());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        ApiResponse<Void> body =
                ApiResponse.failure(
                        ex.errorCode().code(), ex.getMessage(), TraceIdHolder.current().value());
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }

    /**
     * 提取 Bearer 令牌；格式非法按未携带处理。
     *
     * @param request 当前请求。
     * @return 令牌原文；无合法头时 null。
     */
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(AUTHORIZATION_HEADER);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return null;
        }
        String token = header.substring(BEARER_PREFIX.length()).trim();
        return token.isEmpty() ? null : token;
    }

    /**
     * 判断请求是否在免认证白名单内。
     *
     * @param request 当前请求。
     * @return true 表示放行。
     */
    private boolean isWhitelisted(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (PREFIX_WHITELIST.stream().anyMatch(path::startsWith)) {
            return true;
        }
        return EXACT_WHITELIST.stream()
                .anyMatch(
                        endpoint ->
                                endpoint.method().matches(request.getMethod())
                                        && endpoint.path().equals(path));
    }

    /** 免认证端点（方法 + 路径）。 */
    private record AnonymousEndpoint(HttpMethod method, String path) {}
}
