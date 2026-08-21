package com.elink.evco.platform.iam.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** IAM 域哈希工具：刷新令牌、幂等指纹等敏感材料仅存 SHA-256 摘要。 */
public final class HashUtils {

    /** 工具类禁止实例化。 */
    private HashUtils() {}

    /**
     * 计算 SHA-256 十六进制摘要。
     *
     * @param value 原文。
     * @return 十六进制摘要字符串。
     */
    public static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16))
                        .append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 算法不可用", ex);
        }
    }
}
