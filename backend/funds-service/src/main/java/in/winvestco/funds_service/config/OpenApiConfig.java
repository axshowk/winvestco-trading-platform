package in.winvestco.funds_service.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI/Swagger configuration for funds service
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI fundsServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("WINVESTCO Funds Service API")
                        .description("API for managing wallets, ledger entries, funds locking, and transactions")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("WINVESTCO Team")
                                .url("https://winvestco.in")
                                .email("support@winvestco.in"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT token from user-service authentication")));
    }
}
