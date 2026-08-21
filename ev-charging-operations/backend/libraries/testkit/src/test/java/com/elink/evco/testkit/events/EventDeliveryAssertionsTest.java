package com.elink.evco.testkit.events;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.elink.evco.testkit.events.EventDeliveryAttempt.Outcome;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 验证无业务消息 Testkit 的通用幂等与顺序约束。 */
class EventDeliveryAssertionsTest {
    /** 验证一次成功后同事件重复投递会被安全标记为重复。 */
    @Test
    @DisplayName("允许已应用事件的重复投递")
    void acceptsDuplicateAfterApplication() {
        var attempts =
                List.of(
                        new EventDeliveryAttempt("event-1", "order-1", 1, Outcome.APPLIED, null),
                        new EventDeliveryAttempt("event-1", "order-1", 2, Outcome.DUPLICATE, null));

        assertThatCode(() -> EventDeliveryAssertions.assertAtLeastOnceSafety(attempts))
                .doesNotThrowAnyException();
    }

    /** 验证尚未持久化过的事件不能被伪装为重复投递。 */
    @Test
    @DisplayName("拒绝无原始应用事实的重复投递")
    void rejectsDuplicateWithoutPriorApplication() {
        var attempts =
                List.of(new EventDeliveryAttempt("event-1", "order-1", 1, Outcome.DUPLICATE, null));

        assertThatThrownBy(() -> EventDeliveryAssertions.assertAtLeastOnceSafety(attempts))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("此前已应用");
    }

    /** 验证同一顺序键的观察序列不得倒退，避免测试错误掩盖乱序风险。 */
    @Test
    @DisplayName("拒绝同一顺序键的倒退序列")
    void rejectsOutOfOrderSequenceWithinPartitionKey() {
        var attempts =
                List.of(
                        new EventDeliveryAttempt("event-1", "order-1", 2, Outcome.APPLIED, null),
                        new EventDeliveryAttempt("event-2", "order-1", 1, Outcome.APPLIED, null));

        assertThatThrownBy(() -> EventDeliveryAssertions.assertAtLeastOnceSafety(attempts))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("严格递增");
    }
}
