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
class SmsNotificationServiceTest {

    @Mock
    private NotificationChannelConfig channelConfig;

    @InjectMocks
    private SmsNotificationService smsService;

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
    void getChannel_shouldReturnSms() {
        assertEquals(DeliveryChannel.SMS, smsService.getChannel());
    }

    @Test
    void isEnabled_whenConfigured_shouldReturnTrue() {
        NotificationChannelConfig.Sms smsConfig = new NotificationChannelConfig.Sms();
        smsConfig.setEnabled(true);
        smsConfig.setTwilioAccountSid("test-sid");
        smsConfig.setTwilioAuthToken("test-token");
        smsConfig.setTwilioFromNumber("+1234567890");
        when(channelConfig.getSms()).thenReturn(smsConfig);

        assertTrue(smsService.isEnabled());
    }

    @Test
    void isEnabled_whenDisabled_shouldReturnFalse() {
        NotificationChannelConfig.Sms smsConfig = new NotificationChannelConfig.Sms();
        smsConfig.setEnabled(false);
        when(channelConfig.getSms()).thenReturn(smsConfig);

        assertFalse(smsService.isEnabled());
    }

    @Test
    void send_whenDisabled_shouldReturnFalse() {
        NotificationChannelConfig.Sms smsConfig = new NotificationChannelConfig.Sms();
        smsConfig.setEnabled(false);
        when(channelConfig.getSms()).thenReturn(smsConfig);

        boolean result = smsService.send(1L, testNotification, "+919876543210");

        assertFalse(result);
    }

    @Test
    void send_withInvalidPhone_shouldReturnFalse() {
        NotificationChannelConfig.Sms smsConfig = new NotificationChannelConfig.Sms();
        smsConfig.setEnabled(true);
        smsConfig.setTwilioAccountSid("test-sid");
        smsConfig.setTwilioAuthToken("test-token");
        smsConfig.setTwilioFromNumber("+1234567890");
        when(channelConfig.getSms()).thenReturn(smsConfig);

        boolean result = smsService.send(1L, testNotification, "invalid");

        assertFalse(result);
    }

    @Test
    void send_withNullDestination_shouldReturnFalse() {
        NotificationChannelConfig.Sms smsConfig = new NotificationChannelConfig.Sms();
        smsConfig.setEnabled(true);
        smsConfig.setTwilioAccountSid("test-sid");
        smsConfig.setTwilioAuthToken("test-token");
        smsConfig.setTwilioFromNumber("+1234567890");
        when(channelConfig.getSms()).thenReturn(smsConfig);

        boolean result = smsService.send(1L, testNotification, null);

        assertFalse(result);
    }

    @Test
    void send_withSecurityNotificationType_shouldBeEligible() {
        NotificationDTO securityNotification = NotificationDTO.builder()
                .id(2L)
                .userId(1L)
                .type(NotificationType.USER_LOGIN)
                .title("Login Alert")
                .message("New login detected")
                .build();

        // Security notifications should be SMS-eligible
        assertTrue(securityNotification.getType() == NotificationType.USER_LOGIN);
    }

    @Test
    void isValidDestination_withValidPhone_shouldReturnTrue() {
        assertTrue(smsService.isValidDestination("+919876543210"));
        assertTrue(smsService.isValidDestination("+1234567890"));
    }

    @Test
    void isValidDestination_withInvalidPhone_shouldReturnFalse() {
        assertFalse(smsService.isValidDestination("123"));
        assertFalse(smsService.isValidDestination(""));
        assertFalse(smsService.isValidDestination(null));
        assertFalse(smsService.isValidDestination("not-a-number"));
    }

    @Test
    void getDisplayName_shouldReturnSmsChannel() {
        assertEquals("SMS", smsService.getDisplayName());
    }
}
