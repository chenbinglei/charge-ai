package com.elink.evco.web.security;

import com.elink.evco.kernel.api.ApiResponse;
import com.elink.evco.web.config.WebSecurityProperties;
import com.elink.evco.web.error.BusinessException;
import com.elink.evco.web.error.PlatformErrorCode;
import com.elink.evco.web.trace.TraceIdHolder;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Bearer 令牌过滤器：解析 JWT → 校验会话有效性（端口回源）→ 绑定认证上下文；白名单路径（配置声明）直接放行， 其余未携带令牌的请求以匿名身份进入，由权限拦截器或端点按需拒绝。
 *
 * <p>过滤器异常不在 @RestControllerAdvice 覆盖范围内，因此就地输出统一 ApiResponse 外观， 保证令牌失败路径与业务失败路径外观一致。
 */
public class BearerAuthFilter extends OncePerRequestFilter {

    /** 认证头。 */
    private static final String AUTHORIZATION_HEADER = "Authorization";

    /** Bearer 方案前缀。 */
    private static final String BEARER_PREFIX = "Bearer ";

    /** JWT 签发器。 */
    private final JwtTokenProvider jwtTokenProvider;

    /** 会话校验端口；由宿主服务实现。 */
    private final SessionValidationPort sessionValidation;

    /** 免认证路径前缀；构造时读取配置固化。 */
    private final List<String> anonymousPrefixes;

    /** 免认证精确端点；构造时解析配置固化。 */
    private final List<AnonymousEndpoint> anonymousEndpoints;

    /** JSON 序列化器；过滤器内失败响应输出。 */
    private final ObjectMapper objectMapper;

    /**
     * 构造认证过滤器并解析免认证配置。
     *
     * @param jwtTokenProvider JWT 签发器。
     * @param sessionValidation 会话校验端口。
     * @param securityProperties 免认证端点配置。
     * @param objectMapper JSON 序列化器。
     */
    public BearerAuthFilter(
            JwtTokenProvider jwtTokenProvider,
            SessionValidationPort sessionValidation,
            WebSecurityProperties securityProperties,
            ObjectMapper objectMapper) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.sessionValidation = sessionValidation;
        this.anonymousPrefixes = List.copyOf(securityProperties.getAnonymousPrefixes());
        this.anonymousEndpoints =
                securityProperties.getAnonymousEndpoints().stream()
                        .map(AnonymousEndpoint::parse)
                        .toList();
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
            Long userId = sessionValidation.validateActive(context.sessionId());
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
    private void writeFailure(HttpServletResponse response, BusinessException ex)
            throws IOException {
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
     * 判断请求是否在免认证白名单内（前缀 + 精确端点，均来自配置）。
     *
     * @param request 当前请求。
     * @return true 表示放行。
     */
    private boolean isWhitelisted(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (anonymousPrefixes.stream().anyMatch(path::startsWith)) {
            return true;
        }
        return anonymousEndpoints.stream()
                .anyMatch(
                        endpoint ->
                                endpoint.method().matches(request.getMethod())
                                        && endpoint.path().equals(path));
    }

    /** 免认证端点（方法 + 路径）。 */
    private record AnonymousEndpoint(HttpMethod method, String path) {

        /**
         * 解析 "METHOD /path" 配置项为端点；格式非法快速失败防止白名单静默失效。
         *
         * @param value 配置字符串。
         * @return 免认证端点。
         */
        static AnonymousEndpoint parse(String value) {
            String[] parts = value.trim().split("\\s+", 2);
            if (parts.length != 2 || parts[1].isBlank()) {
                throw new IllegalArgumentException("免认证端点格式必须为 \"METHOD /path\"，实际：" + value);
            }
            return new AnonymousEndpoint(HttpMethod.valueOf(parts[0].toUpperCase()), parts[1]);
        }
    }
}
