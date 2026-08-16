package com.elink.evco.kernel.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.elink.evco.kernel.context.TraceIdGenerator;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 通用 API 响应与分页结构的确定性单元测试。 */
class ApiResponseTest {
    /** 验证成功响应固定使用统一结果码并保留调用链关联。 */
    @Test
    @DisplayName("统一响应：成功结果使用稳定结果码与追踪标识")
    void successResponseUsesStableCodeAndTraceId() {
        var traceId = TraceIdGenerator.create().value();

        var response = ApiResponse.success("已登记数据", traceId);

        assertThat(response.code()).isEqualTo("SUCCESS");
        assertThat(response.traceId()).isEqualTo(traceId);
        assertThat(response.timestamp()).isNotNull();
    }

    /** 验证失败响应不暴露动态业务数据。 */
    @Test
    @DisplayName("统一响应：失败结果不携带业务数据")
    void failureResponseDoesNotExposeBusinessData() {
        var response =
                ApiResponse.failure(
                        "VALIDATION_ERROR", "参数校验失败", TraceIdGenerator.create().value());

        assertThat(response.data()).isNull();
    }

    /** 验证分页对象拒绝非法页码，避免下游查询产生无边界分页。 */
    @Test
    @DisplayName("分页：拒绝小于一的页码")
    void pageResponseRejectsInvalidPageNumber() {
        assertThatThrownBy(() -> new PageResponse<>(List.of(), 0, 20, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("pageNo 必须从 1 开始");
    }
}
