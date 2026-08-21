package com.elink.evco.gateway.config;

import com.elink.evco.kernel.context.TraceId;
import com.elink.evco.kernel.context.TraceIdGenerator;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** 在网关入口复用合规 TraceId 或生成新 TraceId，并回写响应头供调用方定位问题。 */
@Component
public class TraceIdFilter extends OncePerRequestFilter {
    /** HTTP 请求和响应共用的 TraceId 头名称。 */
    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    /**
     * 为每个请求绑定追踪标识；非法输入不回显，改用新生成的技术标识。
     *
     * @param request 当前 HTTP 请求。
     * @param response 当前 HTTP 响应。
     * @param filterChain 后续过滤器链。
     * @throws ServletException 下游 Servlet 处理失败时抛出。
     * @throws IOException 响应写入失败时抛出。
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        var traceId = resolveTraceId(request.getHeader(TRACE_ID_HEADER));
        response.setHeader(TRACE_ID_HEADER, traceId.value());
        filterChain.doFilter(request, response);
    }

    /**
     * 仅接受符合共享模型约束的上游追踪标识，避免日志与响应头被无效输入污染。
     *
     * @param requestedTraceId 上游提交的追踪标识。
     * @return 可安全传播的追踪标识。
     */
    private TraceId resolveTraceId(String requestedTraceId) {
        if (requestedTraceId == null) {
            return TraceIdGenerator.create();
        }
        try {
            return new TraceId(requestedTraceId);
        } catch (IllegalArgumentException exception) {
            return TraceIdGenerator.create();
        }
    }
}
