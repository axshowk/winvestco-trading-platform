package in.winvestco.notification_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.winvestco.notification_service.dto.NotificationDispatchMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@Slf4j
@RequiredArgsConstructor
public class RedisNotificationSubscriber {

    private final ObjectMapper objectMapper;
    private final WebSocketNotificationService webSocketService;

    /**
     * Handle incoming Redis messages.
     * The method name "onMessage" is configured in RedisConfig's
     * MessageListenerAdapter.
     */
    public void onMessage(String message, String channel) {
        try {
            log.debug("Received Redis notification message on channel: {}", channel);
            NotificationDispatchMessage dispatchMessage = objectMapper.readValue(message,
                    NotificationDispatchMessage.class);

            if (dispatchMessage.isBroadcast()) {
                webSocketService.sendBroadcastLocal(dispatchMessage.getNotification());
            } else if (dispatchMessage.getUserId() != null) {
                webSocketService.sendToUserLocal(dispatchMessage.getUserId(), dispatchMessage.getNotification());
            }
        } catch (IOException e) {
            log.error("Failed to parse Redis notification message: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Error processing Redis notification: {}", e.getMessage(), e);
        }
    }
}
