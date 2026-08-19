package com.elink.evco.platform.common.security;

import com.elink.evco.platform.common.config.AppProperties;
import com.elink.evco.platform.common.error.BusinessException;
import com.elink.evco.platform.common.error.PlatformErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

/**
 * access_token（JWT）签发与校验；HS256 对称签名，密钥来自配置且必须 ≥32 字节。
 *
 * <p>令牌声明：uid（用户 ID）、sid（会话 ID）、utype（用户类型）、tenant（租户）、
 * uname（登录名）、dname（展示名）、authtype（PASSWORD/SSO）；会话有效性由
 * SessionService 在过滤器内校验，令牌本身只证明签发事实。
 */
@Component
public class JwtTokenProvider {

    /** HS256 最小密钥长度（字节）。 */
    private static final int MIN_SECRET_BYTES = 32;

    /** 签名密钥；构造时派生并缓存。 */
    private final SecretKey secretKey;

    /** 业务配置。 */
    private final AppProperties appProperties;

    /**
     * 构造令牌签发器并校验密钥长度，防止弱密钥静默上线。
     *
     * @param appProperties 平台业务配置。
     */
    public JwtTokenProvider(AppProperties appProperties) {
        String secret = appProperties.getAuth().getJwtSecret();
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("jwt-secret 必须至少 " + MIN_SECRET_BYTES + " 字节");
        }
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.appProperties = appProperties;
    }

    /**
     * 为已建立的会话签发 access_token。
     *
     * @param context 会话对应的认证上下文。
     * @return 签名后的 JWT 字符串。
     */
    public String createAccessToken(AuthContext context) {
        Instant now = Instant.now();
        Duration ttl = Duration.ofMinutes(appProperties.getAuth().getAccessTokenTtlMinutes());
        return Jwts.builder()
                .subject(String.valueOf(context.userId()))
                .claim("uid", context.userId())
                .claim("sid", context.sessionId())
                .claim("utype", context.userType())
                .claim("tenant", context.tenantId())
                .claim("uname", context.username())
                .claim("dname", context.displayName())
                .claim("authtype", context.authType())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(secretKey)
                .compact();
    }

    /**
     * 解析并验签 access_token；过期与篡改分别翻译为登记错误码。
     *
     * @param token Bearer 令牌原文。
     * @return 解析出的认证上下文。
     */
    public AuthContext parse(String token) {
        Claims claims;
        try {
            claims = Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
        } catch (ExpiredJwtException ex) {
            throw new BusinessException(PlatformErrorCode.AUTH_TOKEN_EXPIRED);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new BusinessException(PlatformErrorCode.AUTH_TOKEN_INVALID);
        }
        return new AuthContext(
                claims.get("uid", Long.class),
                claims.get("sid", Long.class),
                claims.get("uname", String.class),
                claims.get("dname", String.class),
                claims.get("tenant", Long.class),
                claims.get("utype", String.class),
                claims.get("authtype", String.class));
    }

    /** @return access_token 有效期（分钟）。 */
    public long accessTokenTtlMinutes() {
        return appProperties.getAuth().getAccessTokenTtlMinutes();
    }
}
