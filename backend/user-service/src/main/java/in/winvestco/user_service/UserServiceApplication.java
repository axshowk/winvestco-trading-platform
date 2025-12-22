package in.winvestco.user_service;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

import in.winvestco.common.util.LoggingUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.PostConstruct;

@Slf4j
@SpringBootApplication(scanBasePackages = {
        "in.winvestco.user_service",
        "in.winvestco.common"
})
@EnableDiscoveryClient
@EnableFeignClients
@EnableCaching
@EnableScheduling
@EnableRabbit
@RequiredArgsConstructor
public class UserServiceApplication {

    private final LoggingUtils loggingUtils;

    @PostConstruct
    public void init() {
        loggingUtils.setServiceName("user-service");
        log.info("UserServiceApplication initialized with centralized logging and service discovery");
    }

    public static void main(String[] args) {
        log.info("Starting UserServiceApplication with centralized logging...");
        SpringApplication.run(UserServiceApplication.class, args);
        log.info("UserServiceApplication started successfully with centralized logging");
    }
}
