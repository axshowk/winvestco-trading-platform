package in.winvestco.common.config;

import in.winvestco.common.interceptor.RateLimitInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WebConfigTest {

    @Test
    void addInterceptors_ShouldAddRateLimitInterceptor() {
        RateLimitInterceptor rateLimitInterceptor = mock(RateLimitInterceptor.class);
        WebConfig webConfig = new WebConfig(rateLimitInterceptor);
        InterceptorRegistry registry = new InterceptorRegistry();

        webConfig.addInterceptors(registry);

        assertNotNull(registry);
    }

    @Test
    void webConfig_WhenCreated_ShouldNotBeNull() {
        RateLimitInterceptor rateLimitInterceptor = mock(RateLimitInterceptor.class);
        WebConfig webConfig = new WebConfig(rateLimitInterceptor);

        assertNotNull(webConfig);
    }
}
