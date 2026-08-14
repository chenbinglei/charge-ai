package com.elink.evco.integration.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** W0 process entrypoint; external adapters and channels are not started here. */
@SpringBootApplication(scanBasePackages = "com.elink.evco.integration")
public class IntegrationServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(IntegrationServiceApplication.class, args);
    }
}
