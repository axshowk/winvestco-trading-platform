package in.winvestco.notification_service.service.channel;

import in.winvestco.notification_service.config.NotificationChannelConfig;
import in.winvestco.notification_service.dto.NotificationDTO;
import in.winvestco.notification_service.model.DeliveryChannel;
import in.winvestco.notification_service.model.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * SMS notification delivery service.
 * Supports Twilio and AWS SNS providers.
 * 
 * Note: SMS should only be used for critical/security notifications
 * due to cost and user experience considerations.
 * 
 * To fully enable SMS:
 * 1. Add twilio or aws-sns dependency to pom.xml
 * 2. Configure provider credentials in application.yml
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SmsNotificationService implements ChannelDeliveryService {

    // E.164 phone number format: +[country code][number]
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[1-9]\\d{9,14}$");

    // Notification types that warrant SMS delivery
    private static final Set<NotificationType> SMS_ELIGIBLE_TYPES = Set.of(
            NotificationType.USER_LOGIN,
            NotificationType.USER_PASSWORD_CHANGED,
            NotificationType.USER_STATUS_CHANGED,
            NotificationType.ORDER_REJECTED,
            NotificationType.TRADE_FAILED);

    private final NotificationChannelConfig config;

    @Override
    public DeliveryChannel getChannel() {
        return DeliveryChannel.SMS;
    }

    @Override
    public boolean isEnabled() {
        NotificationChannelConfig.Sms smsConfig = config.getSms();
        return smsConfig.isEnabled() &&
                (smsConfig.getTwilioAccountSid() != null ||
                        "aws-sns".equals(smsConfig.getProvider()));
    }

    @Override
    public boolean send(Long userId, NotificationDTO notification, String phoneNumber) {
        if (!isEnabled()) {
            log.debug("SMS notifications are disabled, skipping delivery");
            return false;
        }

        if (!isValidDestination(phoneNumber)) {
            log.warn("Invalid phone number for user {}: {}", userId, maskPhoneNumber(phoneNumber));
            return false;
        }

        // Check if notification type is eligible for SMS
        if (!isSmsEligible(notification.getType())) {
            log.debug("Notification type {} is not eligible for SMS delivery", notification.getType());
            return false;
        }

        try {
            log.info("Sending SMS notification to user {} at {}", userId, maskPhoneNumber(phoneNumber));

            String smsContent = buildSmsContent(notification);

            // Use configured provider
            String provider = config.getSms().getProvider();
            boolean sent = switch (provider.toLowerCase()) {
                case "twilio" -> sendViaTwilio(phoneNumber, smsContent);
                case "aws-sns" -> sendViaAwsSns(phoneNumber, smsContent);
                default -> {
                    log.error("Unknown SMS provider: {}", provider);
                    yield false;
                }
            };

            if (sent) {
                log.info("SMS notification sent successfully to user {}", userId);
            }
            return sent;

        } catch (Exception e) {
            log.error("Failed to send SMS notification to user {}: {}", userId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean isValidDestination(String phoneNumber) {
        return phoneNumber != null && PHONE_PATTERN.matcher(phoneNumber).matches();
    }

    @Override
    public String getDisplayName() {
        return "SMS";
    }

    /**
     * Check if notification type is eligible for SMS delivery.
     */
    public boolean isSmsEligible(NotificationType type) {
        return SMS_ELIGIBLE_TYPES.contains(type);
    }

    /**
     * Build SMS content from notification.
     * SMS has a 160 character limit, so we need to be concise.
     */
    private String buildSmsContent(NotificationDTO notification) {
        String content = String.format("[WinvestCo] %s: %s",
                notification.getTitle(),
                notification.getMessage());

        int maxLength = config.getSms().getMaxLength();
        if (content.length() > maxLength) {
            content = content.substring(0, maxLength - 3) + "...";
        }

        return content;
    }

    /**
     * Mask phone number for logging (show only last 4 digits).
     */
    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 4) {
            return "****";
        }
        return "****" + phoneNumber.substring(phoneNumber.length() - 4);
    }

    /**
     * Send SMS via Twilio.
     */
    private boolean sendViaTwilio(String to, String message) {
        // In production, use Twilio SDK:
        // Twilio.init(config.getSms().getTwilioAccountSid(),
        // config.getSms().getTwilioAuthToken());
        // Message twilioMessage = Message.creator(
        // new PhoneNumber(to),
        // new PhoneNumber(config.getSms().getTwilioFromNumber()),
        // message
        // ).create();
        // return twilioMessage.getStatus() != Message.Status.FAILED;

        log.info("Twilio SMS would be sent to: {} with message: {}", maskPhoneNumber(to), message);
        return true; // Simulated success
    }

    /**
     * Send SMS via AWS SNS.
     */
    private boolean sendViaAwsSns(String to, String message) {
        // In production, use AWS SDK:
        // SnsClient snsClient = SnsClient.builder()
        // .region(Region.of(config.getSms().getAwsRegion()))
        // .build();
        // PublishRequest request = PublishRequest.builder()
        // .message(message)
        // .phoneNumber(to)
        // .messageAttributes(Map.of(
        // "AWS.SNS.SMS.SMSType", MessageAttributeValue.builder()
        // .stringValue("Transactional")
        // .dataType("String")
        // .build()
        // ))
        // .build();
        // snsClient.publish(request);

        log.info("AWS SNS SMS would be sent to: {} with message: {}", maskPhoneNumber(to), message);
        return true; // Simulated success
    }
}
