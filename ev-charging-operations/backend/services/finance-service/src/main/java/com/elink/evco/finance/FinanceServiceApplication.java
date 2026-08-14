package com.elink.evco.finance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** W0 服务启动入口；资金行为与数据模型尚不在本阶段范围内。 */
@SpringBootApplication(scanBasePackages = "com.elink.evco.finance")
public class FinanceServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(FinanceServiceApplication.class, args);
    }
}
