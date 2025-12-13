package in.winvestco.common.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
@ConditionalOnClass(name = "org.springframework.data.redis.core.RedisTemplate")
public class RedisService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * Store a value in Redis with expiration time
     */
    public void set(String key, Object value, Duration duration) {
        ValueOperations<String, Object> ops = redisTemplate.opsForValue();
        ops.set(key, value, duration);
    }

    /**
     * Store a value in Redis with expiration time in seconds
     */
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        ValueOperations<String, Object> ops = redisTemplate.opsForValue();
        ops.set(key, value, timeout, unit);
    }

    /**
     * Get a value from Redis
     */
    public Object get(String key) {
        ValueOperations<String, Object> ops = redisTemplate.opsForValue();
        return ops.get(key);
    }

    /**
     * Delete a key from Redis
     */
    public Boolean delete(String key) {
        return redisTemplate.delete(key);
    }

    /**
     * Check if key exists in Redis
     */
    public Boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }

    /**
     * Set expiration time for a key
     */
    public Boolean expire(String key, Duration duration) {
        return redisTemplate.expire(key, duration);
    }

    /**
     * Increment a numeric value in Redis
     */
    public Long increment(String key) {
        ValueOperations<String, Object> ops = redisTemplate.opsForValue();
        return ops.increment(key);
    }

    /**
     * Increment a numeric value in Redis by a specific amount
     */
    public Long increment(String key, long delta) {
        ValueOperations<String, Object> ops = redisTemplate.opsForValue();
        return ops.increment(key, delta);
    }

    /**
     * Store user session data
     */
    public void storeSessionData(String sessionId, String userId, Object sessionData, Duration duration) {
        String key = "session:" + sessionId + ":user:" + userId;
        set(key, sessionData, duration);
    }

    /**
     * Get user session data
     */
    public Object getSessionData(String sessionId, String userId) {
        String key = "session:" + sessionId + ":user:" + userId;
        return get(key);
    }

    /**
     * Store rate limiting data
     */
    public void storeRateLimitData(String key, Object data, Duration duration) {
        set("rate_limit:" + key, data, duration);
    }

    /**
     * Get rate limiting data
     */
    public Object getRateLimitData(String key) {
        return get("rate_limit:" + key);
    }

    /**
     * Store temporary data with short expiration
     */
    public void storeTemporaryData(String key, Object data) {
        set(key, data, Duration.ofMinutes(5));
    }

    /**
     * Get temporary data
     */
    public Object getTemporaryData(String key) {
        return get(key);
    }
}
