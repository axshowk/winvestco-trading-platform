package in.winvestco.notification_service.service.channel;

import in.winvestco.notification_service.config.NotificationChannelConfig;
import in.winvestco.notification_service.dto.NotificationDTO;
import in.winvestco.notification_service.model.DeliveryChannel;
import in.winvestco.notification_service.model.NotificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PushNotificationServiceTest {

    @Mock
    private NotificationChannelConfig channelConfig;

    @InjectMocks
    private PushNotificationService pushService;

    private NotificationDTO testNotification;

    @BeforeEach
    void setUp() {
        testNotification = NotificationDTO.builder()
                .id(1L)
                .userId(1L)
                .type(NotificationType.TRADE_EXECUTED)
                .title("Trade Executed")
                .message("Your trade for RELIANCE was executed at ₹2500")
                .build();
    }

    @Test
    void getChannel_shouldReturnPush() {
        assertEquals(DeliveryChannel.PUSH, pushService.getChannel());
    }

    @Test
    void isEnabled_whenConfigured_shouldReturnTrue() {
        NotificationChannelConfig.Push pushConfig = new NotificationChannelConfig.Push();
        pushConfig.setEnabled(true);
        pushConfig.setFirebaseProjectId("test-project-id");
        when(channelConfig.getPush()).thenReturn(pushConfig);

        assertTrue(pushService.isEnabled());
    }

    @Test
    void isEnabled_whenDisabled_shouldReturnFalse() {
        NotificationChannelConfig.Push pushConfig = new NotificationChannelConfig.Push();
        pushConfig.setEnabled(false);
        when(channelConfig.getPush()).thenReturn(pushConfig);

        assertFalse(pushService.isEnabled());
    }

    @Test
    void isEnabled_whenMissingProjectId_shouldReturnFalse() {
        NotificationChannelConfig.Push pushConfig = new NotificationChannelConfig.Push();
        pushConfig.setEnabled(true);
        pushConfig.setFirebaseProjectId(null);
        when(channelConfig.getPush()).thenReturn(pushConfig);

        assertFalse(pushService.isEnabled());
    }

    @Test
    void send_whenDisabled_shouldReturnFalse() {
        NotificationChannelConfig.Push pushConfig = new NotificationChannelConfig.Push();
        pushConfig.setEnabled(false);
        when(channelConfig.getPush()).thenReturn(pushConfig);

        boolean result = pushService.send(1L, testNotification, "fcm-token-123");

        assertFalse(result);
    }

    @Test
    void send_withNullToken_shouldReturnFalse() {
        NotificationChannelConfig.Push pushConfig = new NotificationChannelConfig.Push();
        pushConfig.setEnabled(true);
        pushConfig.setFirebaseProjectId("test-project-id");
        when(channelConfig.getPush()).thenReturn(pushConfig);

        boolean result = pushService.send(1L, testNotification, null);

        assertFalse(result);
    }

    @Test
    void send_withEmptyToken_shouldReturnFalse() {
        NotificationChannelConfig.Push pushConfig = new NotificationChannelConfig.Push();
        pushConfig.setEnabled(true);
        pushConfig.setFirebaseProjectId("test-project-id");
        when(channelConfig.getPush()).thenReturn(pushConfig);

        boolean result = pushService.send(1L, testNotification, "");

        assertFalse(result);
    }

    @Test
    void isValidDestination_withValidToken_shouldReturnTrue() {
        assertTrue(pushService.isValidDestination("dOxJH_F2RLW:APA91bH_valid_token_example_12345"));
    }

    @Test
    void isValidDestination_withInvalidToken_shouldReturnFalse() {
        assertFalse(pushService.isValidDestination(""));
        assertFalse(pushService.isValidDestination(null));
    }

    @Test
    void getDisplayName_shouldReturnPushChannel() {
        assertEquals("PUSH", pushService.getDisplayName());
    }
}
