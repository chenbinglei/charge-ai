package com.elink.evco.gateway.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** W0 process entrypoint; routing capabilities are introduced only after W1 contracts. */
@SpringBootApplication(scanBasePackages = "com.elink.evco.gateway")
public class ApiGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
