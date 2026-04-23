package com.melodyshop.engagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(scanBasePackages = {"com.melodyshop.engagement", "com.melodyshop.common"})
@EnableDiscoveryClient
public class CustomerEngagementServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(CustomerEngagementServiceApplication.class, args);
    }
}
