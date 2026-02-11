package in.winvestco.order_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {
                "in.winvestco.order_service",
                "in.winvestco.common"
})
@EnableDiscoveryClient
@EnableFeignClients
@EnableScheduling
public class OrderServiceApplication {

        public static void main(String[] args) {
                SpringApplication.run(OrderServiceApplication.class, args);
        }
}
