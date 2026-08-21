package com.elink.evco.platform.iam.service;

import com.elink.evco.platform.iam.dto.SsoTicketPushRequest;
import com.elink.evco.platform.iam.vo.LoginVO;

/**
 * SSO 免登录服务（仅 IOT 协同模式启用）： IOT 推送一次性 ticket（SET EX 120 NX 唯一写入）→ 前端跳转 /sso/login → GETDEL
 * 原子消费换会话；并发同 ticket 有且仅有一个成功，防重放； Redis 故障 fail-closed；ticket 原文不落日志/审计（仅 SHA-256 指纹前 8 位）。
 */
public interface SsoService {

    /**
     * IOT 推送一次性 SSO ticket（服务间接口）；仅 IOT 协同模式启用。
     *
     * @param request 推送请求（ticket、iotUserId、expireAt）。
     * @param apiKeyHeader 请求携带的 X-API-Key。
     */
    void pushTicket(SsoTicketPushRequest request, String apiKeyHeader);

    /**
     * SSO 免登录：ticket 换会话；仅识别已推送用户（source=IOT_PUSH、status=active）， 本地不存在或不可用返回
     * SSO_USER_NOT_FOUND（ticket 保持已消费，不回滚复活）。
     *
     * @param ticket 一次性票据原文。
     * @param ip 来源 IP。
     * @param userAgent 登录端 User-Agent。
     * @return 登录响应（与密码登录令牌结构一致）。
     */
    LoginVO ssoLogin(String ticket, String ip, String userAgent);
}
