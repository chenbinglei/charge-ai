package com.elink.evco.web.trace;

import com.elink.evco.kernel.context.TraceId;

/**
 * 当前请求链路追踪标识的线程级持有器；由 {@link TraceIdFilter} 写入和清理。
 *
 * <p>控制器与业务层通过本类读取当前 traceId 填充 ApiResponse 与审计日志， 避免在方法签名间手工传递；过滤器负责在请求结束时清理，防止线程池串号。
 */
public final class TraceIdHolder {
    /** 线程本地存储；Web 容器线程池复用必须成对 set/clear。 */
    private static final ThreadLocal<TraceId> CURRENT = new ThreadLocal<>();

    /** 工具类禁止实例化。 */
    private TraceIdHolder() {}

    /**
     * 绑定当前请求的追踪标识。
     *
     * @param traceId 过滤器解析或生成的追踪标识。
     */
    public static void set(TraceId traceId) {
        CURRENT.set(traceId);
    }

    /** 清理线程本地值；请求结束必须调用，防止线程池串号。 */
    public static void clear() {
        CURRENT.remove();
    }

    /**
     * @return 当前请求追踪标识；过滤器外（定时任务等）返回固定占位值。
     */
    public static TraceId current() {
        TraceId traceId = CURRENT.get();
        return traceId != null ? traceId : new TraceId("0000000000000000");
    }
}
