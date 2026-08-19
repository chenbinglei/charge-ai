package com.elink.evco.platform.iam.service;

import com.elink.evco.platform.common.cache.RedisKeys;
import com.elink.evco.platform.common.error.BusinessException;
import com.elink.evco.platform.common.error.PlatformErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 幂等键服务：以请求体指纹做 Redis SET NX 去重；同键同指纹重放首次成功响应，
 * 同键异指纹返回 IDEMPOTENCY_CONFLICT；Redis 故障时降级放行（幂等非安全控制）。
 */
@Service
public class IdempotencyService {

    /** 受控日志；Redis 故障降级记录告警。 */
    private static final Logger LOG = LoggerFactory.getLogger(IdempotencyService.class);

    /** Redis 客户端。 */
    private final StringRedisTemplate redis;

    /** Redis 键规则。 */
    private final RedisKeys keys;

    /** JSON 序列化。 */
    private final ObjectMapper objectMapper;

    /**
     * 构造幂等服务。
     *
     * @param redis Redis 客户端。
     * @param keys Redis 键规则。
     * @param objectMapper JSON 序列化器。
     */
    public IdempotencyService(StringRedisTemplate redis, RedisKeys keys, ObjectMapper objectMapper) {
        this.redis = redis;
        this.keys = keys;
        this.objectMapper = objectMapper;
    }

    /**
     * 幂等前置检查；返回缓存的首个成功响应供直接重放。
     *
     * @param scope 幂等作用域（如 iam-user-create、auth-login）。
     * @param idempotencyKey 调用方携带的 X-Idempotency-Key；空白表示跳过幂等。
     * @param requestBody 请求体（用于指纹计算）。
     * @return 空表示继续执行；非空为可重放的响应 JSON。
     */
    public Optional<String> begin(String scope, String idempotencyKey, Object requestBody) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Optional.empty();
        }
        String fingerprint = sha256Hex(toJson(requestBody));
        String fingerprintKey = keys.idemFingerprint(scope, idempotencyKey.trim());
        try {
            Boolean first = redis.opsForValue().setIfAbsent(fingerprintKey, fingerprint, Duration.ofHours(24));
            if (Boolean.TRUE.equals(first)) {
                return Optional.empty();
            }
            String existing = redis.opsForValue().get(fingerprintKey);
            if (existing != null && existing.equals(fingerprint)) {
                String cached = redis.opsForValue().get(keys.idemResponse(scope, idempotencyKey.trim()));
                if (cached != null) {
                    return Optional.of(cached);
                }
                // 首个同指纹请求仍在处理中；拒绝并发重复，防止重复创建。
                throw new BusinessException(
                        PlatformErrorCode.IDEMPOTENCY_CONFLICT, "相同幂等键请求处理中，请稍后重试");
            }
            throw new BusinessException(PlatformErrorCode.IDEMPOTENCY_CONFLICT);
        } catch (BusinessException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            LOG.warn("幂等缓存不可用，降级放行 scope={} key={}", scope, idempotencyKey, ex);
            return Optional.empty();
        }
    }

    /**
     * 缓存首个成功响应；供同键同指纹重放。
     *
     * @param scope 幂等作用域。
     * @param idempotencyKey 幂等键。
     * @param responseBody 成功响应体。
     */
    public void complete(String scope, String idempotencyKey, Object responseBody) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return;
        }
        try {
            redis.opsForValue()
                    .set(keys.idemResponse(scope, idempotencyKey.trim()), toJson(responseBody), Duration.ofHours(24));
        } catch (RuntimeException ex) {
            LOG.warn("幂等响应缓存失败 scope={} key={}", scope, idempotencyKey, ex);
        }
    }

    /**
     * 序列化对象为 JSON；序列化失败按指纹冲突前置换为错误占位。
     *
     * @param value 对象。
     * @return JSON 字符串。
     */
    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(PlatformErrorCode.VALIDATION_ERROR, "请求体不可序列化");
        }
    }

    /**
     * 计算 SHA-256 十六进制摘要。
     *
     * @param value 原文。
     * @return 十六进制摘要。
     */
    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 算法不可用", ex);
        }
    }
}
