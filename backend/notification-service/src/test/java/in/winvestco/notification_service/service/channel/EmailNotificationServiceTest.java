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
class EmailNotificationServiceTest {

    @Mock
    private NotificationChannelConfig channelConfig;

    @InjectMocks
    private EmailNotificationService emailService;

    private NotificationDTO testNotification;

    @BeforeEach
    void setUp() {
        testNotification = NotificationDTO.builder()
                .id(1L)
                .userId(1L)
                .type(NotificationType.ORDER_CREATED)
                .title("Order Placed")
                .message("Your order for RELIANCE has been placed")
                .build();
    }

    @Test
    void getChannel_shouldReturnEmail() {
        assertEquals(DeliveryChannel.EMAIL, emailService.getChannel());
    }

    @Test
    void isEnabled_whenConfigured_shouldReturnTrue() {
        NotificationChannelConfig.Email emailConfig = new NotificationChannelConfig.Email();
        emailConfig.setEnabled(true);
        emailConfig.setSendgridApiKey("test-api-key");
        when(channelConfig.getEmail()).thenReturn(emailConfig);

        assertTrue(emailService.isEnabled());
    }

    @Test
    void isEnabled_whenDisabled_shouldReturnFalse() {
        NotificationChannelConfig.Email emailConfig = new NotificationChannelConfig.Email();
        emailConfig.setEnabled(false);
        when(channelConfig.getEmail()).thenReturn(emailConfig);

        assertFalse(emailService.isEnabled());
    }

    @Test
    void send_whenDisabled_shouldReturnFalse() {
        NotificationChannelConfig.Email emailConfig = new NotificationChannelConfig.Email();
        emailConfig.setEnabled(false);
        when(channelConfig.getEmail()).thenReturn(emailConfig);

        boolean result = emailService.send(1L, testNotification, "user@example.com");

        assertFalse(result);
    }

    @Test
    void send_withInvalidEmail_shouldReturnFalse() {
        NotificationChannelConfig.Email emailConfig = new NotificationChannelConfig.Email();
        emailConfig.setEnabled(true);
        emailConfig.setSendgridApiKey("test-api-key");
        when(channelConfig.getEmail()).thenReturn(emailConfig);

        boolean result = emailService.send(1L, testNotification, "invalid-email");

        assertFalse(result);
    }

    @Test
    void send_withNullDestination_shouldReturnFalse() {
        NotificationChannelConfig.Email emailConfig = new NotificationChannelConfig.Email();
        emailConfig.setEnabled(true);
        emailConfig.setSendgridApiKey("test-api-key");
        when(channelConfig.getEmail()).thenReturn(emailConfig);

        boolean result = emailService.send(1L, testNotification, null);

        assertFalse(result);
    }

    @Test
    void isValidDestination_withValidEmail_shouldReturnTrue() {
        assertTrue(emailService.isValidDestination("user@example.com"));
        assertTrue(emailService.isValidDestination("user.name+tag@domain.co.in"));
    }

    @Test
    void isValidDestination_withInvalidEmail_shouldReturnFalse() {
        assertFalse(emailService.isValidDestination("invalid"));
        assertFalse(emailService.isValidDestination(""));
        assertFalse(emailService.isValidDestination(null));
    }

    @Test
    void getDisplayName_shouldReturnEmailChannel() {
        assertEquals("EMAIL", emailService.getDisplayName());
    }
}
