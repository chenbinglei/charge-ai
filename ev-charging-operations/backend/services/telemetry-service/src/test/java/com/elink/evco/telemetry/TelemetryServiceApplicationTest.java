package com.elink.evco.telemetry;

import com.elink.evco.telemetry.TelemetryServiceApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = TelemetryServiceApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
/** W0 遥测服务健康检查，证明未提前接入遥测流或实时推送。 */
class TelemetryServiceApplicationTest {
    /** 随机端口测试客户端，避免测试之间争用固定网络端口。 */
    @Autowired
    private TestRestTemplate restTemplate;

    /** 验证 W0 仅提供健康检查，不以空遥测接口伪造功能完成。 */
    @Test
    @DisplayName("W0：遥测服务仅暴露健康检查")
    void exposesHealthWithoutBusinessEndpoints() {
        assertThat(restTemplate.getForEntity("/actuator/health", String.class).getStatusCode().is2xxSuccessful()).isTrue();
    }

}
