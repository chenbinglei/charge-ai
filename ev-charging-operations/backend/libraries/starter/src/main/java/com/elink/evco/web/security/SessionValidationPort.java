package com.elink.evco.web.security;

/**
 * 会话有效性校验端口：由具体服务实现，供 Bearer 令牌过滤器回源校验。
 *
 * <p>过滤器只负责令牌解析与上下文绑定；会话存储方式（Redis + DB 等）由实现方决定。
 */
public interface SessionValidationPort {

    /**
     * 校验会话是否有效。
     *
     * @param sessionId JWT 中的会话 ID 声明。
     * @return 有效时返回会话归属用户 ID；无效返回 null。
     */
    Long validateActive(Long sessionId);
}
