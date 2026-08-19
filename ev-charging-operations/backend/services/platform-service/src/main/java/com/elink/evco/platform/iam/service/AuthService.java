package com.elink.evco.platform.iam.service;

import com.elink.evco.platform.iam.dto.LoginRequest;
import com.elink.evco.platform.iam.vo.LoginVO;
import com.elink.evco.platform.iam.vo.ProfileVO;
import com.elink.evco.web.security.AuthContext;

/**
 * 认证服务：图形验证码登录（5 次失败锁 30 分钟）、登出、刷新（一次性 refresh_token 轮换）、被动撤销与 profile（权限/菜单/部署模式）。
 *
 * <p>失败路径不区分「用户不存在」与「密码错误」；锁定/停用状态分别提示但不泄露其他账号信息。
 */
public interface AuthService {

    /**
     * 管理端账号密码登录；验证码一次性校验，成功签发令牌对并回写登录事实。
     *
     * @param request 登录请求。
     * @param ip 来源 IP。
     * @param userAgent 登录端 User-Agent。
     * @return 登录响应（令牌对、角色、部署模式）。
     */
    LoginVO login(LoginRequest request, String ip, String userAgent);

    /**
     * 主动登出：撤销会话与刷新令牌，清理权限缓存并审计。
     *
     * @param refreshToken 刷新令牌原文。
     */
    void logout(String refreshToken);

    /**
     * 刷新 access_token：refresh_token 一次性使用并轮换新令牌对。
     *
     * @param refreshToken 刷新令牌原文。
     * @return 新令牌对登录响应。
     */
    LoginVO refresh(String refreshToken);

    /**
     * 被动撤销（管理员强制下线）；需审计 token_revoke。
     *
     * @param sessionId 会话 ID。
     * @param reason 撤销原因。
     * @param operator 操作者上下文。
     */
    void revoke(Long sessionId, String reason, AuthContext operator);

    /**
     * 当前用户档案：基础信息、权限码、部署模式与后端推导的可见菜单树。
     *
     * @param userId 用户 ID。
     * @return 档案响应。
     */
    ProfileVO profile(Long userId);
}
