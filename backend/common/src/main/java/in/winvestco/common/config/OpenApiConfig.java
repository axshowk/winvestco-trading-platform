package in.winvestco.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

/**
 * Centralized OpenAPI/Swagger Configuration
 * Provides consistent API documentation across all services
 */
@Configuration("commonOpenApiConfig")
public class OpenApiConfig {

        @Value("${spring.application.name:Winvestco-Trading}")
        private String applicationName;

        @Value("${app.description:Microservices-based Trading Application}")
        private String appDescription;

        @Value("${app.version:1.0.0}")
        private String appVersion;

        @Value("${spring.profiles.active:dev}")
        private String activeProfile;

        @Value("${app.api.base-url:http://localhost:9090}")
        private String baseUrl;

        @Bean
        public GroupedOpenApi userApi() {
                return GroupedOpenApi.builder()
                                .group("User Service")
                                .packagesToScan("com.winvestco.user_service.controller")
                                .pathsToMatch("/api/users/**", "/api/auth/**")
                                .addOpenApiCustomizer(globalHeadersCustomizer())
                                .addOpenApiCustomizer(globalResponsesCustomizer())
                                .build();
        }

        @Bean
        public OpenAPI customOpenAPI() {
                final String securitySchemeName = "bearerAuth";

                return new OpenAPI()
                                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                                .components(new Components()
                                                .addSecuritySchemes(securitySchemeName,
                                                                new SecurityScheme()
                                                                                .name(securitySchemeName)
                                                                                .type(SecurityScheme.Type.HTTP)
                                                                                .scheme("bearer")
                                                                                .bearerFormat("JWT")
                                                                                .description(
                                                                                                "JWT Authorization header using the Bearer scheme. Example: \"Authorization: Bearer {token}\"")))
                                .info(getApiInfo())
                                .servers(getServers())
                                .tags(getTags());
        }

        private List<Server> getServers() {
                return List.of(
                                new Server()
                                                .url(baseUrl)
                                                .description(activeProfile.toUpperCase() + " Environment"),
                                new Server()
                                                .url("https://api.winvestco.com")
                                                .description("Production Environment"));
        }

        private List<Tag> getTags() {
                return List.of(
                                new Tag().name("User Management")
                                                .description("APIs for user authentication and profile management"));
        }

        private OpenApiCustomizer globalHeadersCustomizer() {
                return openApi -> {
                        openApi.getPaths().values().forEach(pathItem -> pathItem.readOperations().forEach(operation -> {
                                // Add Accept-Language header
                                operation.addParametersItem(new HeaderParameter()
                                                .name("Accept-Language")
                                                .description("Language preference")
                                                .required(false)
                                                .schema(new Schema<String>().type("string")._default("en-US")));
                        }));
                };
        }

        private OpenApiCustomizer globalResponsesCustomizer() {
                return openApi -> {
                        openApi.getPaths().values().forEach(pathItem -> pathItem.readOperations().forEach(operation -> {
                                // Add common error responses
                                operation.getResponses()
                                                .addApiResponse(String.valueOf(HttpStatus.BAD_REQUEST.value()),
                                                                createApiResponse("Bad Request"))
                                                .addApiResponse(String.valueOf(HttpStatus.UNAUTHORIZED.value()),
                                                                createApiResponse("Unauthorized"))
                                                .addApiResponse(String.valueOf(HttpStatus.FORBIDDEN.value()),
                                                                createApiResponse("Forbidden"))
                                                .addApiResponse(String.valueOf(HttpStatus.NOT_FOUND.value()),
                                                                createApiResponse("Not Found"))
                                                .addApiResponse(String
                                                                .valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()),
                                                                createApiResponse("Internal Server Error"));
                        }));
                };
        }

        private ApiResponse createApiResponse(String message) {
                return new ApiResponse()
                                .description(message)
                                .content(new Content().addMediaType(
                                                org.springframework.http.MediaType.APPLICATION_JSON_VALUE,
                                                new MediaType().schema(new Schema<Map<String, Object>>()
                                                                .addProperties("error", new Schema<Object>()
                                                                                .type("object")
                                                                                .addProperty("status",
                                                                                                new Schema<Integer>()
                                                                                                                .type("integer")
                                                                                                                .example(400))
                                                                                .addProperty("error",
                                                                                                new Schema<String>()
                                                                                                                .type("string")
                                                                                                                .example("Bad Request"))
                                                                                .addProperty("message",
                                                                                                new Schema<String>()
                                                                                                                .type("string")
                                                                                                                .example(message))
                                                                                .addProperty("path",
                                                                                                new Schema<String>()
                                                                                                                .type("string")
                                                                                                                .example("/api/endpoint"))
                                                                                .addProperty("timestamp",
                                                                                                new Schema<String>()
                                                                                                                .type("string")
                                                                                                                .format("date-time"))))));
        }

        private Info getApiInfo() {
                return new Info()
                                .title(applicationName + " API")
                                .version(appVersion)
                                .description(appDescription)
                                .contact(new Contact()
                                                .name("Winvestco Development Team")
                                                .email("dev@winvestco.com")
                                                .url("https://winvestco.com"))
                                .license(new License()
                                                .name("Proprietary")
                                                .url("https://winvestco.com/terms"))
                                .termsOfService("https://winvestco.com/terms");
        }
}
