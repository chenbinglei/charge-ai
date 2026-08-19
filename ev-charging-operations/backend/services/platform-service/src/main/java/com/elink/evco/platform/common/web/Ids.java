package com.elink.evco.platform.common.web;

import com.elink.evco.platform.common.error.BusinessException;
import com.elink.evco.platform.common.error.PlatformErrorCode;

/**
 * 雪花 ID 与字符串的转换工具：API 报文一律以字符串承载 18 位 ID，存储层用 bigint。
 *
 * <p>非法输入统一翻译为 VALIDATION_ERROR，不在错误信息中回显原值。
 */
public final class Ids {

    /** 工具类禁止实例化。 */
    private Ids() {}

    /**
     * 解析字符串形式的雪花 ID。
     *
     * @param value 报文中的 ID 字符串。
     * @param fieldName 字段名，用于错误提示定位。
     * @return 解析后的 Long 值。
     */
    public static Long parse(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(PlatformErrorCode.VALIDATION_ERROR, fieldName + " 不能为空");
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ex) {
            throw new BusinessException(PlatformErrorCode.VALIDATION_ERROR, fieldName + " 格式不合法");
        }
    }

    /**
     * 输出 ID 的字符串形式；null 安全。
     *
     * @param id 数值 ID。
     * @return 字符串形式；入参为 null 时返回 null。
     */
    public static String stringify(Long id) {
        return id == null ? null : String.valueOf(id);
    }
}
