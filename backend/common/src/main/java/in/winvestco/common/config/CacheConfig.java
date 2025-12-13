package in.winvestco.common.config;

import org.springframework.boot.autoconfigure.cache.CacheManagerCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheManager;
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
    public CacheManagerCustomizer<RedisCacheManager> cacheManagerCustomizer() {
        return cacheManager -> {
            // Configure default cache settings
            RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                    .entryTtl(Duration.ofMinutes(10)) // Default TTL: 10 minutes
                    .serializeValuesWith(RedisSerializationContext.SerializationPair
                            .fromSerializer(new GenericJackson2JsonRedisSerializer()));

            // Configure specific cache settings for different entities
            Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

            // User cache - longer TTL since user data doesn't change often
            cacheConfigurations.put("users", defaultConfig.entryTtl(Duration.ofMinutes(30)));

            // Market data cache - shorter TTL for real-time data
            cacheConfigurations.put("marketData", defaultConfig.entryTtl(Duration.ofMinutes(5)));

            // Portfolio cache - medium TTL
            cacheConfigurations.put("portfolio", defaultConfig.entryTtl(Duration.ofMinutes(15)));

            // Account cache - medium TTL
            cacheConfigurations.put("accounts", defaultConfig.entryTtl(Duration.ofMinutes(20)));

            // Order cache - short TTL for frequently changing data
            cacheConfigurations.put("orders", defaultConfig.entryTtl(Duration.ofMinutes(5)));

            // Holdings cache - medium TTL
            cacheConfigurations.put("holdings", defaultConfig.entryTtl(Duration.ofMinutes(10)));
            cacheConfigurations.forEach(
                    (cacheName, config) -> cacheManager.getCacheConfigurations().putIfAbsent(cacheName, config));
        };
    }
}
