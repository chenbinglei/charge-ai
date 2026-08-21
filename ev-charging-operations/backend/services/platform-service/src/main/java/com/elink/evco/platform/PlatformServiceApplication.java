package com.elink.evco.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** W0 服务启动入口；业务模块须经 W1 及后续门禁批准后才可引入。 */
@SpringBootApplication(scanBasePackages = "com.elink.evco.platform")
public class PlatformServiceApplication {
    /**
     * 启动平台服务；W0 不创建 IAM、订单、资产或运营业务路由。
     *
     * @param args Spring Boot 启动参数。
     */
    public static void main(String[] args) {
        SpringApplication.run(PlatformServiceApplication.class, args);
    }
}
