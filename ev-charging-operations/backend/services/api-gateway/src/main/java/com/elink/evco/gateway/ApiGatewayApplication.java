package com.elink.evco.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** W0 服务启动入口；路由能力须在 W1 契约评审通过后才可引入。 */
@SpringBootApplication(scanBasePackages = "com.elink.evco.gateway")
public class ApiGatewayApplication {
    /**
     * 启动网关服务；W0 仅验证健康检查，业务路由须在后续契约评审后创建。
     *
     * @param args Spring Boot 启动参数。
     */
    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
