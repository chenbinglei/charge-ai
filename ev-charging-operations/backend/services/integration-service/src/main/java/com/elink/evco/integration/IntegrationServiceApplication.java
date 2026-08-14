package com.elink.evco.integration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** W0 服务启动入口；外部适配器和渠道能力尚不在本阶段范围内。 */
@SpringBootApplication(scanBasePackages = "com.elink.evco.integration")
public class IntegrationServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(IntegrationServiceApplication.class, args);
    }
}
