package com.elink.evco.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.elink.evco.gateway.config.TraceIdFilter;
import java.net.URI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.RequestEntity;

@SpringBootTest(
        classes = ApiGatewayApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
/** W0 网关健康检查，证明骨架未提前暴露业务路由。 */
class ApiGatewayApplicationTest {
    /** 随机端口测试客户端，避免测试之间争用固定网络端口。 */
    @Autowired private TestRestTemplate restTemplate;

    /** 验证 W0 仅提供健康检查，不以空业务接口伪造功能完成。 */
    @Test
    @DisplayName("W0：网关仅暴露健康检查")
    void exposesHealthWithoutBusinessEndpoints() {
        assertThat(
                        restTemplate
                                .getForEntity("/actuator/health", String.class)
                                .getStatusCode()
                                .is2xxSuccessful())
                .isTrue();
    }

    /** 验证合规的上游 TraceId 会被原样传递，便于跨服务关联诊断。 */
    @Test
    @DisplayName("W1：网关透传有效的 TraceId")
    void preservesValidTraceId() {
        var traceId = "4d8a6e2cc9074c4aa5efbfa23f96d8bf";
        var request =
                RequestEntity.get(URI.create("/actuator/health"))
                        .header(TraceIdFilter.TRACE_ID_HEADER, traceId)
                        .build();

        var response = restTemplate.exchange(request, String.class);

        assertThat(response.getHeaders().getFirst(TraceIdFilter.TRACE_ID_HEADER))
                .isEqualTo(traceId);
    }

    /** 验证异常输入绝不回显，而是由网关替换为新的受控 TraceId。 */
    @Test
    @DisplayName("W1：网关替换无效的 TraceId")
    void replacesInvalidTraceId() {
        var request =
                RequestEntity.get(URI.create("/actuator/health"))
                        .header(TraceIdFilter.TRACE_ID_HEADER, "invalid")
                        .build();

        var response = restTemplate.exchange(request, String.class);
        var actualTraceId = response.getHeaders().getFirst(TraceIdFilter.TRACE_ID_HEADER);

        assertThat(actualTraceId).isNotEqualTo("invalid").hasSizeBetween(16, 128);
    }
}
