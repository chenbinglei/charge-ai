package com.elink.evco.kernel.idempotency;

import java.util.Objects;

/**
 * 有副作用命令的服务端幂等范围；单独的客户端键不足以防止跨主体或异载荷冲突。
 *
 * @param actorReference 已脱敏的调用主体引用。
 * @param endpoint 稳定的版本化接口标识。
 * @param key 客户端提交的幂等键。
 * @param requestFingerprint 规范化请求体的不可逆摘要。
 */
public record IdempotencyScope(
        String actorReference, String endpoint, IdempotencyKey key, String requestFingerprint) {
    /** 规范化并校验范围字段，禁止将原始敏感请求写入幂等记录。 */
    public IdempotencyScope {
        actorReference = requireText(actorReference, "actorReference");
        endpoint = requireText(endpoint, "endpoint");
        key = Objects.requireNonNull(key, "key 不能为空");
        requestFingerprint = requireText(requestFingerprint, "requestFingerprint");
    }

    /**
     * 校验范围字段为非空白文本。
     *
     * @param value 待校验文本。
     * @param fieldName 技术字段名称。
     * @return 去除首尾空白后的文本。
     */
    private static String requireText(String value, String fieldName) {
        var normalized = Objects.requireNonNull(value, fieldName + " 不能为空").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " 不能为空白");
        }
        return normalized;
    }
}
