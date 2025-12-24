package in.winvestco.schedule_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableDiscoveryClient
@EnableScheduling
@ComponentScan(basePackages = { "in.winvestco.common", "in.winvestco.schedule_service" })
@EnableJpaRepositories(basePackages = { "in.winvestco.common", "in.winvestco.schedule_service" })
@EntityScan(basePackages = { "in.winvestco.common", "in.winvestco.schedule_service" })
public class ScheduleServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ScheduleServiceApplication.class, args);
    }
}
