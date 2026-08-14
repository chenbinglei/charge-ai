package com.elink.evco.ecosystem;

import com.elink.evco.ecosystem.EcosystemServiceApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = EcosystemServiceApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
/** W0 监管服务健康检查，证明未提前创建监管或外部生态能力。 */
class EcosystemServiceApplicationTest {
    /** 随机端口测试客户端，避免测试之间争用固定网络端口。 */
    @Autowired
    private TestRestTemplate restTemplate;

    /** 验证 W0 仅提供健康检查，不以空监管接口伪造功能完成。 */
    @Test
    @DisplayName("W0：监管服务仅暴露健康检查")
    void exposesHealthWithoutBusinessEndpoints() {
        assertThat(restTemplate.getForEntity("/actuator/health", String.class).getStatusCode().is2xxSuccessful()).isTrue();
    }

}
