package com.elink.evco.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** W0 服务启动入口；路由能力须在 W1 契约评审通过后才可引入。 */
@SpringBootApplication(scanBasePackages = "com.elink.evco.gateway")
public class ApiGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
