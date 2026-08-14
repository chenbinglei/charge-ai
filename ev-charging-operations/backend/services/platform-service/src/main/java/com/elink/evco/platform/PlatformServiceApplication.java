package com.elink.evco.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** W0 服务启动入口；业务模块须经 W1 及后续门禁批准后才可引入。 */
@SpringBootApplication(scanBasePackages = "com.elink.evco.platform")
public class PlatformServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(PlatformServiceApplication.class, args);
    }
}
