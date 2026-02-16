package in.winvestco.risk_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class RiskServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(RiskServiceApplication.class, args);
    }
}
