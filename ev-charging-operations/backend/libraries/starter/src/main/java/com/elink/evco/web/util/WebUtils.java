package com.elink.evco.web.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Web 请求工具：从当前请求提取客户端信息的静态方法集合。
 *
 * <p>仅供控制器/服务薄层使用；不含业务规则，避免控制器内堆积私有封装方法。
 */
public final class WebUtils {

    /** 代理转发链路头；取第一跳作为客户端真实 IP。 */
    private static final String FORWARDED_FOR_HEADER = "X-Forwarded-For";

    /** 工具类禁止实例化。 */
    private WebUtils() {}

    /**
     * 提取客户端真实 IP；优先取代理链第一跳，头缺失时回退远端地址。
     *
     * @param request 当前请求。
     * @return 客户端 IP 字符串。
     */
    public static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader(FORWARDED_FOR_HEADER);
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
