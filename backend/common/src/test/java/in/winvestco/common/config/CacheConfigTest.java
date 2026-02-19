package in.winvestco.common.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class CacheConfigTest {

    private final CacheConfig cacheConfig = new CacheConfig();

    @Test
    void cacheConfiguration_ShouldReturnConfigurationWithDefaultTtl() {
        RedisCacheConfiguration config = cacheConfig.cacheConfiguration();

        assertNotNull(config);
        assertNotNull(config.getTtlFunction());
    }

    @Test
    void redisCacheManagerBuilderCustomizer_ShouldConfigureAllCaches() {
        RedisCacheConfiguration baseConfig = cacheConfig.cacheConfiguration();
        var customizer = cacheConfig.redisCacheManagerBuilderCustomizer(baseConfig);

        assertNotNull(customizer);

        var builder = RedisCacheManager.builder();
        customizer.customize(builder);
        var manager = builder.build();

        assertNotNull(manager.getCache("users"));
        assertNotNull(manager.getCache("marketData"));
        assertNotNull(manager.getCache("portfolio"));
        assertNotNull(manager.getCache("accounts"));
        assertNotNull(manager.getCache("orders"));
        assertNotNull(manager.getCache("holdings"));
    }

    @Test
    void cacheConfiguration_ShouldUseJsonSerializer() {
        RedisCacheConfiguration config = cacheConfig.cacheConfiguration();

        RedisSerializationContext.SerializationPair<?> pair = ReflectionTestUtils.invokeMethod(
                config, "getValueSerializationPair");
        assertNotNull(pair);
    }
}
