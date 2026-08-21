package com.elink.evco.kernel.audit;

import com.elink.evco.kernel.context.AuditActor;
import com.elink.evco.kernel.context.TraceId;
import java.time.Instant;
import java.util.Objects;

/**
 * 可在各服务持久化的最小审计事实；业务详情由所属服务在自有 Schema 内补充。
 *
 * @param actor 动作执行主体的脱敏引用。
 * @param action 稳定动作名称。
 * @param objectReference 被操作对象的非敏感引用。
 * @param result 动作结果码。
 * @param traceId 关联链路标识。
 * @param occurredAt 动作发生的 UTC 时间。
 */
public record AuditEntry(
        AuditActor actor,
        String action,
        String objectReference,
        String result,
        TraceId traceId,
        Instant occurredAt) {
    /** 校验最小审计字段，避免在缺少主体、对象或追踪关系时写入不可用审计记录。 */
    public AuditEntry {
        actor = Objects.requireNonNull(actor, "actor 不能为空");
        action = requireText(action, "action");
        objectReference = requireText(objectReference, "objectReference");
        result = requireText(result, "result");
        traceId = Objects.requireNonNull(traceId, "traceId 不能为空");
        occurredAt = Objects.requireNonNull(occurredAt, "occurredAt 不能为空");
    }

    /**
     * 校验审计文本字段为非空白文本。
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
