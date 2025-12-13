package in.winvestco.order_service.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        scheme = "bearer"
)
public class OpenApiConfig {

    @Value("${server.port:8089}")
    private String serverPort;

    @Bean
    public OpenAPI orderServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("WINVESTCO Order Service API")
                        .description("Order management service for WINVESTCO Platform - handles order lifecycle, validation, and event emission")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("WINVESTCO Team")
                                .email("support@winvestco.in"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://winvestco.in")))
                .servers(List.of(
                        new Server().url("http://localhost:" + serverPort).description("Local Development"),
                        new Server().url("http://order-service:" + serverPort).description("Docker")
                ));
    }
}
