package com.elink.evco.finance.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** W0 process entrypoint; finance behavior and data models are out of scope. */
@SpringBootApplication(scanBasePackages = "com.elink.evco.finance")
public class FinanceServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(FinanceServiceApplication.class, args);
    }
}
