package in.winvestco.trade_service.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI/Swagger configuration for Trade Service.
 */
@Configuration("tradeServiceOpenApiConfig")
@OpenAPIDefinition(info = @Info(title = "WINVESTCO Trade Service API", version = "1.0.0", description = """
                Trade Service API for managing trade lifecycle.

                **Responsibilities:**
                - Accept trade intent from validated orders
                - Validate trade business rules
                - Manage trade state machine (CREATED → VALIDATED → PLACED → EXECUTED → CLOSED)
                - Trigger execution via events
                - Emit trade lifecycle events

                **Does NOT:**
                - Deduct money directly (funds-service)
                - Maintain portfolio balances (portfolio-service)
                - Fetch market data (market-service)
                - Calculate permanent P&L (portfolio-service)
                """, contact = @Contact(name = "WINVESTCO Support", email = "support@winvestco.in", url = "https://winvestco.in"), license = @License(name = "MIT", url = "https://opensource.org/licenses/MIT")), servers = {
                @Server(url = "http://localhost:8092", description = "Local Development"),
                @Server(url = "http://localhost:8090/api", description = "Via API Gateway")
})
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {
}
