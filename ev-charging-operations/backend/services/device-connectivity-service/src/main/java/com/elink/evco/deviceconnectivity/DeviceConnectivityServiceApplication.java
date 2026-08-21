package com.elink.evco.deviceconnectivity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** W0 服务启动入口；设备协议和 MQTT 能力尚不在本阶段范围内。 */
@SpringBootApplication(scanBasePackages = "com.elink.evco.deviceconnectivity")
public class DeviceConnectivityServiceApplication {
    /**
     * 启动设备连接服务；W0 不建立设备协议、MQTT 消费者或控制命令。
     *
     * @param args Spring Boot 启动参数。
     */
    public static void main(String[] args) {
        SpringApplication.run(DeviceConnectivityServiceApplication.class, args);
    }
}
