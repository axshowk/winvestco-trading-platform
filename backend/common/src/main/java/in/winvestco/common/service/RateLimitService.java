package in.winvestco.common.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
@ConditionalOnClass(name = "org.springframework.data.redis.core.RedisTemplate")
@ConditionalOnBean(RedisService.class)
public class RateLimitService {

    @Autowired
    private RedisService redisService;

    public static class RateLimitConfig {
        private final int maxRequests;
        private final Duration window;

        public RateLimitConfig(int maxRequests, Duration window) {
            this.maxRequests = maxRequests;
            this.window = window;
        }

        public int getMaxRequests() {
            return maxRequests;
        }

        public Duration getWindow() {
            return window;
        }
    }

    /**
     * Check if request is allowed based on rate limiting rules
     */
    public boolean isAllowed(String key, RateLimitConfig config) {
        String redisKey = "rate_limit:" + key;
        long currentTime = Instant.now().getEpochSecond();

        // Get current count from Redis
        Object currentData = redisService.get(redisKey);

        RateLimitData data;
        if (currentData instanceof RateLimitData) {
            data = (RateLimitData) currentData;
        } else {
            data = new RateLimitData(currentTime, 1);
        }

        // Check if window has expired
        if (currentTime - data.getWindowStart() >= config.getWindow().getSeconds()) {
            data = new RateLimitData(currentTime, 1);
        } else {
            // Increment count within the same window
            data = new RateLimitData(data.getWindowStart(), data.getCount() + 1);
        }

        // Store updated data in Redis
        redisService.set(redisKey, data, config.getWindow());

        return data.getCount() <= config.getMaxRequests();
    }

    /**
     * Check if request is allowed with default configuration
     */
    public boolean isAllowed(String key) {
        RateLimitConfig config = getDefaultConfig(key);
        return isAllowed(key, config);
    }

    /**
     * Get default rate limit configuration based on key pattern
     */
    private RateLimitConfig getDefaultConfig(String key) {
        if (key.startsWith("api:auth")) {
            return new RateLimitConfig(5, Duration.ofMinutes(15)); // 5 login attempts per 15 minutes
        } else if (key.startsWith("api:trade")) {
            return new RateLimitConfig(100, Duration.ofMinutes(1)); // 100 trades per minute
        } else if (key.startsWith("api:account")) {
            return new RateLimitConfig(50, Duration.ofMinutes(1)); // 50 account operations per minute
        } else if (key.startsWith("api:user")) {
            return new RateLimitConfig(30, Duration.ofMinutes(1)); // 30 user operations per minute
        } else {
            return new RateLimitConfig(60, Duration.ofMinutes(1)); // Default: 60 requests per minute
        }
    }

    /**
     * Get remaining requests for a key
     */
    public int getRemainingRequests(String key) {
        RateLimitConfig config = getDefaultConfig(key);
        String redisKey = "rate_limit:" + key;
        Object currentData = redisService.get(redisKey);

        if (currentData instanceof RateLimitData) {
            RateLimitData data = (RateLimitData) currentData;
            return Math.max(0, config.getMaxRequests() - data.getCount());
        }

        return config.getMaxRequests();
    }

    /**
     * Get time until rate limit resets (in seconds)
     */
    public long getResetTime(String key) {
        RateLimitConfig config = getDefaultConfig(key);
        String redisKey = "rate_limit:" + key;
        Object currentData = redisService.get(redisKey);

        if (currentData instanceof RateLimitData) {
            RateLimitData data = (RateLimitData) currentData;
            long windowEnd = data.getWindowStart() + config.getWindow().getSeconds();
            return Math.max(0, windowEnd - Instant.now().getEpochSecond());
        }

        return 0;
    }

    /**
     * Rate limit data structure for Redis storage
     */
    public static class RateLimitData {
        private final long windowStart;
        private final int count;

        public RateLimitData(long windowStart, int count) {
            this.windowStart = windowStart;
            this.count = count;
        }

        public long getWindowStart() {
            return windowStart;
        }

        public int getCount() {
            return count;
        }
    }
}
