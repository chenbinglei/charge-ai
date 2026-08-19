package com.elink.evco.platform.common.security;

/**
 * 当前请求认证上下文的线程级持有器；由 BearerAuthFilter 写入并在请求结束清理。
 *
 * <p>业务层只读；非 Web 场景（定时任务等）值为 null，由调用方自行判定未认证。
 */
public final class AuthContextHolder {

    /** 线程本地存储；Web 容器线程池复用必须成对 set/clear。 */
    private static final ThreadLocal<AuthContext> CURRENT = new ThreadLocal<>();

    /** 工具类禁止实例化。 */
    private AuthContextHolder() {}

    /**
     * 绑定当前请求的认证上下文。
     *
     * @param context 过滤器解析出的认证上下文。
     */
    public static void set(AuthContext context) {
        CURRENT.set(context);
    }

    /** 清理线程本地值；请求结束必须调用，防止线程池串号。 */
    public static void clear() {
        CURRENT.remove();
    }

    /** @return 当前请求认证上下文；未认证时为 null。 */
    public static AuthContext current() {
        return CURRENT.get();
    }

    /**
     * 读取当前认证上下文；未认证时抛出 UNAUTHENTICATED 业务异常。
     *
     * @return 非空认证上下文。
     */
    public static AuthContext require() {
        AuthContext context = CURRENT.get();
        if (context == null) {
            throw new com.elink.evco.platform.common.error.BusinessException(
                    com.elink.evco.platform.common.error.PlatformErrorCode.UNAUTHENTICATED);
        }
        return context;
    }
}
