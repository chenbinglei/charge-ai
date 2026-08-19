package com.elink.evco.web.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 平台业务配置（application.yml 的 app 前缀）；环境专有值全部经环境变量注入。
 *
 * <p>字段含义与默认值必须与 application.yml 注释保持一致；运行期只读，不提供动态刷新。
 */
@Setter
@Getter
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    /** 运行环境标识；用于 Redis 键前缀 evco:{env}:… 的环境隔离。 */
    private String env = "local";

    /** 部署模式：部署时确定，运行期不变。 */
    private DeploymentMode deploymentMode = DeploymentMode.STANDALONE;

    /** 认证相关配置。 */
    @Getter private final Auth auth = new Auth();

    /** 认证子配置：令牌、验证码、锁定与 SSO 服务间凭证。 */
    @Setter
    @Getter
    public static class Auth {
        /** JWT 签名密钥；生产环境必须注入 ≥32 字节随机值。 */
        private String jwtSecret;

        /** access_token 有效期（分钟），与 Redis 会话键 TTL 对齐。 */
        private long accessTokenTtlMinutes = 30;

        /** refresh_token 有效期（天），一次性使用后轮换。 */
        private long refreshTokenTtlDays = 14;

        /** 图形验证码子配置。 */
        @Getter private final Captcha captcha = new Captcha();

        /** 登录失败锁定子配置。 */
        @Getter private final Lockout lockout = new Lockout();

        /** SSO 服务间凭证子配置。 */
        @Getter private final Sso sso = new Sso();

        /** 图形验证码配置：长度与 Redis 有效期。 */
        @Setter
        @Getter
        public static class Captcha {
            /** 验证码字符数（4-6 位字母数字）。 */
            private int length = 5;

            /** Redis 存储有效期（秒），一次性校验。 */
            private int ttlSeconds = 120;
        }

        /** 登录失败锁定配置：连续失败 N 次锁定 M 分钟。 */
        @Setter
        @Getter
        public static class Lockout {
            /** 连续失败次数上限。 */
            private int maxFails = 5;

            /** 锁定时长（分钟）。 */
            private int lockMinutes = 30;
        }

        /** SSO 服务间凭证配置。 */
        @Setter
        @Getter
        public static class Sso {
            /** IOT 推送 ticket 的 X-API-Key；IOT 模式下为空时 fail-closed。 */
            private String apiKey = "";
        }
    }
}
