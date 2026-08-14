package com.elink.evco.ecosystem.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** W0 process entrypoint; regulatory tasks and external ecosystem capabilities are not implemented here. */
@SpringBootApplication(scanBasePackages = "com.elink.evco.ecosystem")
public class EcosystemServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(EcosystemServiceApplication.class, args);
    }
}
