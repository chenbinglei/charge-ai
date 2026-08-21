package com.elink.evco.platform.iam.service;

import com.elink.evco.platform.iam.entity.IamUser;
import com.elink.evco.web.security.SessionValidationPort;

/**
 * 会话与令牌管理：登录签发（auth_session + refresh_token + JWT access_token）、 有效性校验（Redis 快路径 + DB
 * 回源）、滑动续期与批量撤销。
 *
 * <p>实现 starter 的 SessionValidationPort 端口，供 Bearer 令牌过滤器回源调用。
 */
public interface SessionService extends SessionValidationPort {

    /**
     * 签发的令牌三元组。
     *
     * @param accessToken JWT 访问令牌。
     * @param refreshToken 刷新令牌原文（仅签发时一次性返回）。
     * @param expiresInSeconds 访问令牌有效秒数。
     * @param sessionId 会话 ID。
     */
    record IssuedTokens(
            String accessToken, String refreshToken, long expiresInSeconds, Long sessionId) {}

    /**
     * 为登录成功的用户建立会话并签发令牌对。
     *
     * @param user 登录用户。
     * @param authType 认证方式：PASSWORD/SSO。
     * @param ip 登录来源 IP。
     * @param userAgent 登录端 User-Agent 摘要。
     * @return 令牌三元组（含会话 ID）。
     */
    IssuedTokens issue(IamUser user, String authType, String ip, String userAgent);

    /**
     * 撤销指定用户的全部有效会话与刷新令牌；停用/锁定/删除/强制下线时调用。
     *
     * @param userId 用户 ID。
     */
    void revokeByUser(Long userId);

    /**
     * 撤销单个会话及其刷新令牌；登出与被动撤销共用。
     *
     * @param sessionId 会话 ID。
     */
    void revokeSession(Long sessionId);

    /**
     * 滑动续期：延长会话过期时间并刷新 Redis 键 TTL；refresh 流程调用。
     *
     * @param sessionId 会话 ID。
     */
    void slide(Long sessionId);
}
