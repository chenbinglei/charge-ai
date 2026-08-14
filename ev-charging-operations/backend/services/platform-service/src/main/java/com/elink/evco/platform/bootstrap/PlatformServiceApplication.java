package com.elink.evco.platform.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** W0 process entrypoint; business modules are introduced through approved W1+ gates. */
@SpringBootApplication(scanBasePackages = "com.elink.evco.platform")
public class PlatformServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(PlatformServiceApplication.class, args);
    }
}
