package com.elink.evco.kernel.idempotency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 幂等范围和请求摘要的确定性单元测试。 */
class RequestFingerprintTest {
    /** 验证同一规范化请求始终产生相同摘要，供服务端安全重放结果。 */
    @Test
    @DisplayName("幂等摘要：相同请求得到相同 SHA-256")
    void producesDeterministicFingerprint() {
        var first = RequestFingerprint.sha256("{\"amountMinor\":100}");
        var second = RequestFingerprint.sha256("{\"amountMinor\":100}");

        assertThat(first).isEqualTo(second).hasSize(64);
    }

    /** 验证幂等范围拒绝空主体，避免不同调用方错误共享命令结果。 */
    @Test
    @DisplayName("幂等范围：拒绝空白调用主体")
    void rejectsBlankActorReference() {
        assertThatThrownBy(
                        () ->
                                new IdempotencyScope(
                                        " ",
                                        "/api/v1/example",
                                        new IdempotencyKey("request-001"),
                                        RequestFingerprint.sha256("{}")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("actorReference 不能为空白");
    }
}
