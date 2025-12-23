package in.winvestco.common.config;

import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
@ConditionalOnClass(name = "org.springframework.data.redis.cache.RedisCacheManager")
public class CacheConfig {

    @Bean
    public RedisCacheConfiguration cacheConfiguration() {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10)) // Default TTL: 10 minutes
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()));
    }

    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer(
            RedisCacheConfiguration cacheConfiguration) {
        return builder -> {
            Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

            // User cache - longer TTL since user data doesn't change often
            cacheConfigurations.put("users", cacheConfiguration.entryTtl(Duration.ofMinutes(30)));

            // Market data cache - shorter TTL for real-time data
            cacheConfigurations.put("marketData", cacheConfiguration.entryTtl(Duration.ofMinutes(5)));

            // Portfolio cache - medium TTL
            cacheConfigurations.put("portfolio", cacheConfiguration.entryTtl(Duration.ofMinutes(15)));

            // Account cache - medium TTL
            cacheConfigurations.put("accounts", cacheConfiguration.entryTtl(Duration.ofMinutes(20)));

            // Order cache - short TTL for frequently changing data
            cacheConfigurations.put("orders", cacheConfiguration.entryTtl(Duration.ofMinutes(5)));

            // Holdings cache - medium TTL
            cacheConfigurations.put("holdings", cacheConfiguration.entryTtl(Duration.ofMinutes(10)));

            builder.withInitialCacheConfigurations(cacheConfigurations);
        };
    }
}
