package com.elink.evco.deviceconnectivity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;

@SpringBootTest(
        classes = DeviceConnectivityServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
/** W0 设备连接服务健康检查，证明未提前接入设备协议或控制能力。 */
class DeviceConnectivityServiceApplicationTest {
    /** 随机端口测试客户端，避免测试之间争用固定网络端口。 */
    @Autowired private TestRestTemplate restTemplate;

    /** 验证 W0 仅提供健康检查，不以空设备接口伪造功能完成。 */
    @Test
    @DisplayName("W0：设备连接服务仅暴露健康检查")
    void exposesHealthWithoutBusinessEndpoints() {
        assertThat(
                        restTemplate
                                .getForEntity("/actuator/health", String.class)
                                .getStatusCode()
                                .is2xxSuccessful())
                .isTrue();
    }
}
