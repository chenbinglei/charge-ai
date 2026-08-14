package com.elink.evco.ecosystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** W0 服务启动入口；监管任务和外部生态能力尚不在本阶段范围内。 */
@SpringBootApplication(scanBasePackages = "com.elink.evco.ecosystem")
public class EcosystemServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(EcosystemServiceApplication.class, args);
    }
}
