package com.elink.evco.integration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** W0 服务启动入口；外部适配器和渠道能力尚不在本阶段范围内。 */
@SpringBootApplication(scanBasePackages = "com.elink.evco.integration")
public class IntegrationServiceApplication {
    /**
     * 启动外部集成服务；W0 不连接支付、银行、门禁、开票或通知渠道。
     *
     * @param args Spring Boot 启动参数。
     */
    public static void main(String[] args) {
        SpringApplication.run(IntegrationServiceApplication.class, args);
    }
}
