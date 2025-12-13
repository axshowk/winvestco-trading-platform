package in.winvestco.portfolio_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Portfolio Service Application
 * 
 * Manages user investment portfolios and stock holdings.
 * Listens for UserCreatedEvent to auto-create demo portfolios for new users.
 */
@SpringBootApplication(scanBasePackages = {
    "in.winvestco.portfolio_service",
    "in.winvestco.common"
})
@EnableDiscoveryClient
@EnableFeignClients
@EnableJpaAuditing
public class PortfolioServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PortfolioServiceApplication.class, args);
    }
}
