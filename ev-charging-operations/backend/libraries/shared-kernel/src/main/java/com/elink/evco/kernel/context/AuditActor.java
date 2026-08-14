package com.elink.evco.kernel.context;

import java.util.Objects;

/**
 * 可审计动作执行主体的非敏感引用。
 *
 * @param type 执行主体类型，例如平台管理员、租户成员或系统任务；不得写入认证凭据。
 * @param reference 在主体类型内唯一的脱敏引用；不得写入完整手机号、证件或密钥。
 */
public record AuditActor(String type, String reference) {
    /**
     * 创建审计主体并统一去除首尾空白，防止审计事实出现无法关联的空值。
     */
    public AuditActor {
        type = requireValue(type, "type");
        reference = requireValue(reference, "reference");
    }

    /**
     * 校验审计字段为非空白文本。
     *
     * @param value 待校验字段值。
     * @param fieldName 用于错误提示的字段技术名称。
     * @return 去除首尾空白后的字段值。
     */
    private static String requireValue(String value, String fieldName) {
        var normalized = Objects.requireNonNull(value, fieldName + " 不能为空").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " 不能为空白");
        }
        return normalized;
    }
}
