package com.elink.evco.platform.common.web;

import com.elink.evco.kernel.context.TraceId;
import com.elink.evco.kernel.context.TraceIdGenerator;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 链路追踪过滤器：入口生成或透传 traceId，写入响应头与 MDC，请求结束清理线程状态。
 *
 * <p>优先透传调用方携带的 X-Trace-Id（合法长度 16-128），否则生成新值；
 * traceId 与日志、审计、ApiResponse 关联，属于设计包 §6.1 网关层 TraceId 职责的
 * 服务内落地（网关接入前由本过滤器兜底）。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {
    /** 对外约定的追踪标识请求/响应头名称。 */
    public static final String TRACE_HEADER = "X-Trace-Id";

    /** MDC 键名；日志格式通过该键输出追踪标识。 */
    public static final String MDC_KEY = "traceId";

    /**
     * 解析或生成 traceId 后放行请求，并在响应头回写同一标识。
     *
     * @param request 当前请求。
     * @param response 当前响应。
     * @param filterChain 过滤器链。
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        TraceId traceId = resolve(request);
        TraceIdHolder.set(traceId);
        org.slf4j.MDC.put(MDC_KEY, traceId.value());
        response.setHeader(TRACE_HEADER, traceId.value());
        try {
            filterChain.doFilter(request, response);
        } finally {
            org.slf4j.MDC.remove(MDC_KEY);
            TraceIdHolder.clear();
        }
    }

    /**
     * 透传合法的调用方追踪标识，否则新生成；非法长度按未携带处理，防止日志注入放大。
     *
     * @param request 当前请求。
     * @return 本次请求使用的追踪标识。
     */
    private TraceId resolve(HttpServletRequest request) {
        String incoming = request.getHeader(TRACE_HEADER);
        if (incoming != null && !incoming.isBlank()) {
            try {
                return new TraceId(incoming);
            } catch (IllegalArgumentException ignored) {
                // 长度或内容不合法时按未携带处理，交由新生成值保证日志可关联。
            }
        }
        return TraceIdGenerator.create();
    }
}
