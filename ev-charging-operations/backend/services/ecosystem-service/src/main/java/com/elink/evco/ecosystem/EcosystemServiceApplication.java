package com.elink.evco.ecosystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** W0 服务启动入口；监管任务和外部生态能力尚不在本阶段范围内。 */
@SpringBootApplication(scanBasePackages = "com.elink.evco.ecosystem")
public class EcosystemServiceApplication {
    /**
     * 启动监管服务；W0 不创建监管任务、外部生态协议或设备控制能力。
     *
     * @param args Spring Boot 启动参数。
     */
    public static void main(String[] args) {
        SpringApplication.run(EcosystemServiceApplication.class, args);
    }
}
