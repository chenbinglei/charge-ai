package com.elink.evco.telemetry.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** W0 process entrypoint; telemetry ingestion and fan-out are introduced after W1. */
@SpringBootApplication(scanBasePackages = "com.elink.evco.telemetry")
public class TelemetryServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(TelemetryServiceApplication.class, args);
    }
}
