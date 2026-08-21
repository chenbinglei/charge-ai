package com.elink.evco.testkit.events;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 验证 Kafka 至少一次投递下的通用安全不变量。
 *
 * <p>该工具不创建 Topic、不绑定消费者组，也不包含业务事实；各模块应以其 Inbox、业务唯一键和审计记录吸收重复。
 */
public final class EventDeliveryAssertions {
    /** 禁止实例化纯断言工具。 */
    private EventDeliveryAssertions() {}

    /**
     * 校验重复投递、顺序键和 DLQ 证据的基础约束。
     *
     * @param attempts 按消费者实际观察顺序排列的投递记录。
     */
    public static void assertAtLeastOnceSafety(List<EventDeliveryAttempt> attempts) {
        Objects.requireNonNull(attempts, "attempts 不能为空");
        Map<String, Long> lastSequenceByPartition = new HashMap<>();
        Map<String, String> partitionByEvent = new HashMap<>();
        Map<String, Integer> appliedCountByEvent = new HashMap<>();

        for (var attempt : attempts) {
            var lastSequence =
                    lastSequenceByPartition.put(attempt.partitionKey(), attempt.sequence());
            if (lastSequence != null && attempt.sequence() <= lastSequence) {
                throw new AssertionError("同一 partitionKey 的测试序号必须严格递增：" + attempt.partitionKey());
            }

            var previousPartition =
                    partitionByEvent.putIfAbsent(attempt.eventId(), attempt.partitionKey());
            if (previousPartition != null && !previousPartition.equals(attempt.partitionKey())) {
                throw new AssertionError("同一 eventId 不得跨 partitionKey 投递：" + attempt.eventId());
            }

            if (attempt.outcome() == EventDeliveryAttempt.Outcome.APPLIED) {
                var appliedCount = appliedCountByEvent.merge(attempt.eventId(), 1, Integer::sum);
                if (appliedCount > 1) {
                    throw new AssertionError("同一 eventId 至多一次写入业务事实：" + attempt.eventId());
                }
            }

            if (attempt.outcome() == EventDeliveryAttempt.Outcome.DUPLICATE
                    && !appliedCountByEvent.containsKey(attempt.eventId())) {
                throw new AssertionError("DUPLICATE 必须对应此前已应用的 eventId：" + attempt.eventId());
            }
        }
    }
}
