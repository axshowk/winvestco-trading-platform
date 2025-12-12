package in.winvestco.apigateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Import;

import in.winvestco.common.config.MetricsConfig;
import in.winvestco.common.config.ReactiveLoggingConfig;

import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableDiscoveryClient
@EnableCaching
@Import({ ReactiveLoggingConfig.class, MetricsConfig.class })
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
