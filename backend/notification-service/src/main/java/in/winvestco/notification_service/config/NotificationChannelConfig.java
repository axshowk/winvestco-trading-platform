package in.winvestco.notification_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Data;

/**
 * Configuration properties for multi-channel notification delivery.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "notification.channels")
public class NotificationChannelConfig {

    /**
     * Push notification configuration (Firebase Cloud Messaging).
     */
    private Push push = new Push();

    /**
     * Email notification configuration.
     */
    private Email email = new Email();

    /**
     * SMS notification configuration.
     */
    private Sms sms = new Sms();

    /**
     * General delivery settings.
     */
    private Delivery delivery = new Delivery();

    @Data
    public static class Push {
        /**
         * Enable/disable push notifications.
         */
        private boolean enabled = false;

        /**
         * Firebase project ID.
         */
        private String firebaseProjectId;

        /**
         * Path to Firebase service account credentials JSON.
         */
        private String credentialsPath;

        /**
         * Default notification icon for Android.
         */
        private String defaultIcon = "ic_notification";

        /**
         * Default notification color (hex).
         */
        private String defaultColor = "#6366f1";
    }

    @Data
    public static class Email {
        /**
         * Enable/disable email notifications.
         */
        private boolean enabled = false;

        /**
         * Email provider: sendgrid, ses, smtp.
         */
        private String provider = "sendgrid";

        /**
         * SendGrid API key.
         */
        private String sendgridApiKey;

        /**
         * AWS SES region (if using SES).
         */
        private String awsRegion = "ap-south-1";

        /**
         * Sender email address.
         */
        private String fromAddress = "notifications@winvestco.in";

        /**
         * Sender name.
         */
        private String fromName = "WinvestCo";

        /**
         * Email template directory.
         */
        private String templatePath = "classpath:templates/email/";
    }

    @Data
    public static class Sms {
        /**
         * Enable/disable SMS notifications.
         */
        private boolean enabled = false;

        /**
         * SMS provider: twilio, aws-sns.
         */
        private String provider = "twilio";

        /**
         * Twilio Account SID.
         */
        private String twilioAccountSid;

        /**
         * Twilio Auth Token.
         */
        private String twilioAuthToken;

        /**
         * Twilio sender phone number.
         */
        private String twilioFromNumber;

        /**
         * AWS SNS region (if using SNS).
         */
        private String awsRegion = "ap-south-1";

        /**
         * Maximum SMS length before splitting.
         */
        private int maxLength = 160;
    }

    @Data
    public static class Delivery {
        /**
         * Maximum retry attempts for failed deliveries.
         */
        private int maxRetries = 3;

        /**
         * Initial retry delay in milliseconds.
         */
        private long retryDelayMs = 1000;

        /**
         * Retry backoff multiplier.
         */
        private double retryBackoffMultiplier = 2.0;

        /**
         * Enable async delivery (recommended for production).
         */
        private boolean asyncEnabled = true;

        /**
         * Thread pool size for async delivery.
         */
        private int asyncPoolSize = 10;
    }
}
