package com.elink.evco.kernel.idempotency;

import java.util.Objects;

/**
 * 调用方提供的单一命令防重键，界定重复检测范围。
 *
 * @param value 调用方提交的防重标识；服务端还须结合主体、接口和请求摘要判断重复范围。
 */
public record IdempotencyKey(String value) {
    /** 单个防重键允许的最大字符数，防止异常输入占用防重存储。 */
    private static final int MAX_LENGTH = 128;

    /** 规范化并校验防重键；此校验不替代业务层的幂等记录和结果重放。 */
    public IdempotencyKey {
        value = Objects.requireNonNull(value, "idempotencyKey 不能为空").trim();
        if (value.isEmpty() || value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("idempotencyKey 长度必须为 1 到 128 个字符");
        }
    }
}
