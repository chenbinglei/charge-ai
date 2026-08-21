package com.elink.evco.telemetry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** W0 服务启动入口；遥测接入与分发能力须在 W1 后按门禁引入。 */
@SpringBootApplication(scanBasePackages = "com.elink.evco.telemetry")
public class TelemetryServiceApplication {
    /**
     * 启动遥测服务；W0 不接入遥测流、缓存、实时推送或 ClickHouse。
     *
     * @param args Spring Boot 启动参数。
     */
    public static void main(String[] args) {
        SpringApplication.run(TelemetryServiceApplication.class, args);
    }
}
