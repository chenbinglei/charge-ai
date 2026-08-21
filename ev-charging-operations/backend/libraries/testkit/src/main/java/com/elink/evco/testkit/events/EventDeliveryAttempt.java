package com.elink.evco.testkit.events;

import java.util.Objects;

/**
 * 用于测试消息至少一次投递语义的无业务载荷记录。
 *
 * @param eventId 事件信封中的全局唯一标识。
 * @param partitionKey 与 Kafka key 相同的局部顺序键。
 * @param sequence 同一分区键内递增的测试序号。
 * @param outcome 消费处理的可观察结果。
 * @param failureCode 失败或 DLQ 时登记的稳定原因码；成功时为 {@code null}。
 */
public record EventDeliveryAttempt(
        String eventId, String partitionKey, long sequence, Outcome outcome, String failureCode) {
    /** W1 Testkit 支持的投递处理结果，不映射任何业务状态。 */
    public enum Outcome {
        APPLIED,
        DUPLICATE,
        RETRYABLE_FAILURE,
        DLQ
    }

    /** 确保测试夹具表达可审计的投递事实，而不是含糊的字符串约定。 */
    public EventDeliveryAttempt {
        eventId = requireText(eventId, "eventId");
        partitionKey = requireText(partitionKey, "partitionKey");
        if (sequence < 1) {
            throw new IllegalArgumentException("sequence 必须从 1 开始");
        }
        outcome = Objects.requireNonNull(outcome, "outcome 不能为空");
        if ((outcome == Outcome.RETRYABLE_FAILURE || outcome == Outcome.DLQ)
                && (failureCode == null || failureCode.isBlank())) {
            throw new IllegalArgumentException("失败或 DLQ 必须登记稳定原因码");
        }
        if (failureCode != null) {
            failureCode = requireText(failureCode, "failureCode");
        }
    }

    /** 统一拒绝夹具中的空白技术标识。 */
    private static String requireText(String value, String fieldName) {
        var normalized = Objects.requireNonNull(value, fieldName + " 不能为空").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " 不能为空白");
        }
        return normalized;
    }
}
