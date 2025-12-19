package in.winvestco.common.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Feign configuration to propagate JWT token between services.
 * 
 * When a service makes a Feign call to another service, this interceptor
 * copies the Authorization header from the incoming request to the outgoing
 * request.
 */
@Configuration
@ConditionalOnClass(RequestInterceptor.class)
public class FeignAuthConfig {

    /**
     * Propagates the Authorization header from incoming requests to outgoing Feign
     * requests.
     * This ensures that service-to-service calls maintain the user's authentication
     * context.
     */
    @Bean
    public RequestInterceptor authorizationForwardingInterceptor() {
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate template) {
                ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder
                        .getRequestAttributes();
                if (attributes != null) {
                    HttpServletRequest request = attributes.getRequest();
                    String authHeader = request.getHeader("Authorization");
                    if (authHeader != null && !authHeader.isEmpty()) {
                        template.header("Authorization", authHeader);
                    }
                }
            }
        };
    }
}
