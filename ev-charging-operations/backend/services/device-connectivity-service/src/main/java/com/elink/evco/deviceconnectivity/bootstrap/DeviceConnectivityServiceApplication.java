package com.elink.evco.deviceconnectivity.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** W0 process entrypoint; device protocol and MQTT work remain outside this package. */
@SpringBootApplication(scanBasePackages = "com.elink.evco.deviceconnectivity")
public class DeviceConnectivityServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(DeviceConnectivityServiceApplication.class, args);
    }
}
