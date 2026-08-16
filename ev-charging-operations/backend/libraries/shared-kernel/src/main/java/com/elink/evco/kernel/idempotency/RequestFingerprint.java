package com.elink.evco.kernel.idempotency;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/** 生成请求内容的 SHA-256 摘要，供幂等记录比较，不保存原始请求正文。 */
public final class RequestFingerprint {
    /** 禁止创建工具类实例。 */
    private RequestFingerprint() {}

    /**
     * 计算 UTF-8 请求内容的不可逆摘要。
     *
     * @param normalizedRequest 已完成字段排序和脱敏的规范化请求内容。
     * @return 小写十六进制 SHA-256 摘要。
     */
    public static String sha256(String normalizedRequest) {
        Objects.requireNonNull(normalizedRequest, "normalizedRequest 不能为空");
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of()
                    .formatHex(digest.digest(normalizedRequest.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("运行时缺少 SHA-256 算法", exception);
        }
    }
}
