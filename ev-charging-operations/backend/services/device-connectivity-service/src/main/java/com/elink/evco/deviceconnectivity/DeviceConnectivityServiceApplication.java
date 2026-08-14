package com.elink.evco.deviceconnectivity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** W0 服务启动入口；设备协议和 MQTT 能力尚不在本阶段范围内。 */
@SpringBootApplication(scanBasePackages = "com.elink.evco.deviceconnectivity")
public class DeviceConnectivityServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(DeviceConnectivityServiceApplication.class, args);
    }
}
