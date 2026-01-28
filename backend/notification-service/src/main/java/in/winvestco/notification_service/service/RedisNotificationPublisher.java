package in.winvestco.notification_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.winvestco.notification_service.config.RedisConfig;
import in.winvestco.notification_service.dto.NotificationDTO;
import in.winvestco.notification_service.dto.NotificationDispatchMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class RedisNotificationPublisher {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public void publish(Long userId, NotificationDTO notification) {
        NotificationDispatchMessage message = NotificationDispatchMessage.builder()
                .userId(userId)
                .notification(notification)
                .broadcast(false)
                .build();
        publishMessage(message);
    }

    public void publishBroadcast(NotificationDTO notification) {
        NotificationDispatchMessage message = NotificationDispatchMessage.builder()
                .notification(notification)
                .broadcast(true)
                .build();
        publishMessage(message);
    }

    private void publishMessage(NotificationDispatchMessage message) {
        try {
            String jsonEntry = objectMapper.writeValueAsString(message);
            redisTemplate.convertAndSend(RedisConfig.NOTIFICATION_TOPIC, jsonEntry);
            log.debug("Published notification to Redis topic: {}", RedisConfig.NOTIFICATION_TOPIC);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize notification for Redis publish: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Failed to publish notification to Redis: {}", e.getMessage());
        }
    }
}
