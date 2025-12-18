package in.winvestco.user_service;

import in.winvestco.common.config.CommonSecurityConfig;
import in.winvestco.common.config.LoggingConfig;
import in.winvestco.common.config.MetricsConfig;
import in.winvestco.common.config.RabbitMQConfig;
import in.winvestco.common.controller.StressTestController;
import in.winvestco.common.exception.GlobalExceptionHandler;
import in.winvestco.common.util.LoggingUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Import;
import in.winvestco.common.config.JpaAuditingConfig;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

import jakarta.annotation.PostConstruct;

@Slf4j
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients

@EnableCaching
@EnableScheduling
@EnableRabbit
@Import({ CommonSecurityConfig.class, RabbitMQConfig.class, LoggingConfig.class, MetricsConfig.class,
        GlobalExceptionHandler.class, StressTestController.class, JpaAuditingConfig.class })
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
