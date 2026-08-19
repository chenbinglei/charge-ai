package com.elink.evco.platform.common.cache;

import com.elink.evco.platform.common.config.AppProperties;
import org.springframework.stereotype.Component;

/**
 * Redis 键集中管理：统一 evco:{env}:{domain}:{entity}:{id} 前缀，禁止业务代码拼散键。
 *
 * <p>键名变更只允许在本类登记；TTL 由使用方按业务语义设置。
 */
@Component
public class RedisKeys {

    /** 环境前缀；evco:{env}: 形式。 */
    private final String prefix;

    /**
     * 构造键构建器并固化环境前缀。
     *
     * @param appProperties 平台业务配置。
     */
    public RedisKeys(AppProperties appProperties) {
        this.prefix = "evco:" + appProperties.getEnv() + ":";
    }

    /** @return 图形验证码键前缀（值=小写验证码内容）。 */
    public String captcha(String captchaId) {
        return prefix + "auth:captcha:" + captchaId;
    }

    /** @return 会话有效性键（值=用户 ID；TTL 与 access_token 对齐）。 */
    public String session(Long sessionId) {
        return prefix + "iam:session:" + sessionId;
    }

    /** @return 用户权限列表缓存键（值=权限码集合；TTL 与 access_token 对齐）。 */
    public String permissions(Long userId) {
        return prefix + "iam:perm:" + userId;
    }

    /** @return SSO 一次性 ticket 键（值=IOT 用户 ID；TTL 120 秒，GETDEL 消费）。 */
    public String ssoTicket(String ticket) {
        return prefix + "sso:ticket:" + ticket;
    }

    /** @return 幂等键指纹（值=请求体 SHA-256；NX 写入防并发）。 */
    public String idemFingerprint(String scope, String key) {
        return prefix + "idem:" + scope + ":" + key;
    }

    /** @return 幂等响应缓存键（值=首次成功响应 JSON）。 */
    public String idemResponse(String scope, String key) {
        return prefix + "idem-resp:" + scope + ":" + key;
    }
}
