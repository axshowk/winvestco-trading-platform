package in.winvestco.notification_service.service.channel;

import in.winvestco.notification_service.config.NotificationChannelConfig;
import in.winvestco.notification_service.dto.NotificationDTO;
import in.winvestco.notification_service.model.DeliveryChannel;
import in.winvestco.notification_service.model.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Email notification delivery service.
 * Supports SendGrid and AWS SES providers.
 * 
 * Note: This is a skeleton implementation. To fully enable email:
 * 1. Add sendgrid or aws-ses dependency to pom.xml
 * 2. Configure provider credentials in application.yml
 * 3. Create email templates in resources/templates/email/
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EmailNotificationService implements ChannelDeliveryService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    private final NotificationChannelConfig config;

    @Override
    public DeliveryChannel getChannel() {
        return DeliveryChannel.EMAIL;
    }

    @Override
    public boolean isEnabled() {
        NotificationChannelConfig.Email emailConfig = config.getEmail();
        return emailConfig.isEnabled() &&
                (emailConfig.getSendgridApiKey() != null ||
                        "ses".equals(emailConfig.getProvider()));
    }

    @Override
    public boolean send(Long userId, NotificationDTO notification, String emailAddress) {
        if (!isEnabled()) {
            log.debug("Email notifications are disabled, skipping delivery");
            return false;
        }

        if (!isValidDestination(emailAddress)) {
            log.warn("Invalid email address for user {}: {}", userId, emailAddress);
            return false;
        }

        try {
            log.info("Sending email notification to user {} at {}", userId, emailAddress);

            String subject = buildSubject(notification);
            String htmlContent = buildHtmlContent(notification);
            String textContent = buildTextContent(notification);

            // Use configured provider
            String provider = config.getEmail().getProvider();
            boolean sent = switch (provider.toLowerCase()) {
                case "sendgrid" -> sendViaSendGrid(emailAddress, subject, htmlContent, textContent);
                case "ses" -> sendViaAWSSes(emailAddress, subject, htmlContent, textContent);
                case "smtp" -> sendViaSMTP(emailAddress, subject, htmlContent, textContent);
                default -> {
                    log.error("Unknown email provider: {}", provider);
                    yield false;
                }
            };

            if (sent) {
                log.info("Email notification sent successfully to user {}", userId);
            }
            return sent;

        } catch (Exception e) {
            log.error("Failed to send email notification to user {}: {}", userId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean isValidDestination(String emailAddress) {
        return emailAddress != null && EMAIL_PATTERN.matcher(emailAddress).matches();
    }

    @Override
    public String getDisplayName() {
        return "Email";
    }

    /**
     * Build email subject from notification.
     */
    private String buildSubject(NotificationDTO notification) {
        String prefix = getSubjectPrefix(notification.getType());
        return prefix + notification.getTitle();
    }

    private String getSubjectPrefix(NotificationType type) {
        return switch (type) {
            case ORDER_FILLED, ORDER_PARTIALLY_FILLED, TRADE_EXECUTED -> "✅ ";
            case ORDER_REJECTED, TRADE_FAILED -> "❌ ";
            case ORDER_CANCELLED, ORDER_EXPIRED -> "⚠️ ";
            case USER_LOGIN, USER_PASSWORD_CHANGED, USER_STATUS_CHANGED -> "🔒 ";
            case FUNDS_DEPOSITED -> "💰 ";
            case FUNDS_WITHDRAWN -> "💸 ";
            default -> "";
        };
    }

    /**
     * Build HTML email content from notification.
     */
    private String buildHtmlContent(NotificationDTO notification) {
        // In production, use Thymeleaf or FreeMarker templates
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background: linear-gradient(135deg, #6366f1, #8b5cf6); color: white; padding: 20px; border-radius: 8px 8px 0 0; }
                        .content { background: #f9fafb; padding: 20px; border: 1px solid #e5e7eb; }
                        .footer { background: #f3f4f6; padding: 15px; text-align: center; font-size: 12px; color: #6b7280; border-radius: 0 0 8px 8px; }
                        .button { display: inline-block; background: #6366f1; color: white; padding: 12px 24px; text-decoration: none; border-radius: 6px; margin-top: 15px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1 style="margin:0;">%s</h1>
                        </div>
                        <div class="content">
                            <p>%s</p>
                            <a href="https://app.winvestco.in/notifications" class="button">View Details</a>
                        </div>
                        <div class="footer">
                            <p>This is an automated notification from WinvestCo Trading Platform.</p>
                            <p>© 2026 WinvestCo. All rights reserved.</p>
                        </div>
                    </div>
                </body>
                </html>
                """
                .formatted(notification.getTitle(), notification.getMessage());
    }

    /**
     * Build plain text email content from notification.
     */
    private String buildTextContent(NotificationDTO notification) {
        return """
                %s

                %s

                View details: https://app.winvestco.in/notifications

                ---
                This is an automated notification from WinvestCo Trading Platform.
                """.formatted(notification.getTitle(), notification.getMessage());
    }

    /**
     * Send email via SendGrid.
     */
    private boolean sendViaSendGrid(String to, String subject, String htmlContent, String textContent) {
        // In production, use SendGrid SDK:
        // Email from = new Email(config.getEmail().getFromAddress(),
        // config.getEmail().getFromName());
        // Email toEmail = new Email(to);
        // Content textPart = new Content("text/plain", textContent);
        // Content htmlPart = new Content("text/html", htmlContent);
        // Mail mail = new Mail(from, subject, toEmail, textPart);
        // mail.addContent(htmlPart);
        // SendGrid sg = new SendGrid(config.getEmail().getSendgridApiKey());
        // Request request = new Request();
        // request.setMethod(Method.POST);
        // request.setEndpoint("mail/send");
        // request.setBody(mail.build());
        // Response response = sg.api(request);
        // return response.getStatusCode() >= 200 && response.getStatusCode() < 300;

        log.info("SendGrid email would be sent to: {} with subject: {}", to, subject);
        return true; // Simulated success
    }

    /**
     * Send email via AWS SES.
     */
    private boolean sendViaAWSSes(String to, String subject, String htmlContent, String textContent) {
        // In production, use AWS SDK:
        // SesClient sesClient = SesClient.builder()
        // .region(Region.of(config.getEmail().getAwsRegion()))
        // .build();
        // SendEmailRequest emailRequest = SendEmailRequest.builder()
        // .destination(Destination.builder().toAddresses(to).build())
        // .message(Message.builder()
        // .subject(Content.builder().data(subject).build())
        // .body(Body.builder()
        // .html(Content.builder().data(htmlContent).build())
        // .text(Content.builder().data(textContent).build())
        // .build())
        // .build())
        // .source(config.getEmail().getFromAddress())
        // .build();
        // sesClient.sendEmail(emailRequest);

        log.info("AWS SES email would be sent to: {} with subject: {}", to, subject);
        return true; // Simulated success
    }

    /**
     * Send email via SMTP.
     */
    private boolean sendViaSMTP(String to, String subject, String htmlContent, String textContent) {
        // In production, use JavaMailSender:
        // MimeMessage message = mailSender.createMimeMessage();
        // MimeMessageHelper helper = new MimeMessageHelper(message, true);
        // helper.setTo(to);
        // helper.setSubject(subject);
        // helper.setText(textContent, htmlContent);
        // helper.setFrom(config.getEmail().getFromAddress());
        // mailSender.send(message);

        log.info("SMTP email would be sent to: {} with subject: {}", to, subject);
        return true; // Simulated success
    }
}
