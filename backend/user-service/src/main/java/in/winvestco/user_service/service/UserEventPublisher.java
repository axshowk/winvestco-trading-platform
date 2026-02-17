package in.winvestco.user_service.service;

import in.winvestco.common.config.RabbitMQConfig;
import in.winvestco.common.event.UserCreatedEvent;
import in.winvestco.common.event.UserUpdatedEvent;
import in.winvestco.common.event.UserStatusChangedEvent;
import in.winvestco.common.event.UserRoleChangedEvent;
import in.winvestco.common.event.UserPasswordChangedEvent;
import in.winvestco.common.event.UserLoginEvent;
import in.winvestco.common.messaging.outbox.OutboxService;
import in.winvestco.common.util.LoggingUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Event publisher for user events using the outbox pattern.
 * Events are captured in the outbox table within the same transaction
 * as the data changes, ensuring atomicity and guaranteed delivery.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserEventPublisher {

    private final OutboxService outboxService;
    private final LoggingUtils loggingUtils;

    public void publishUserCreated(UserCreatedEvent event) {
        loggingUtils.logServiceStart("UserEventPublisher", "publishUserCreated", event.getUserId());

        log.info("Capturing UserCreatedEvent in outbox for user: {}", event.getUserId());
        outboxService.captureEvent("User", event.getUserId().toString(),
                RabbitMQConfig.USER_EXCHANGE, "user.created", event);

        loggingUtils.logServiceEnd("UserEventPublisher", "publishUserCreated", event.getUserId());
    }

    public void publishUserUpdated(UserUpdatedEvent event) {
        loggingUtils.logServiceStart("UserEventPublisher", "publishUserUpdated", event.getUserId());

        log.info("Capturing UserUpdatedEvent in outbox for user: {}", event.getUserId());
        outboxService.captureEvent("User", event.getUserId().toString(),
                RabbitMQConfig.USER_EXCHANGE, "user.updated", event);

        loggingUtils.logServiceEnd("UserEventPublisher", "publishUserUpdated", event.getUserId());
    }

    public void publishUserStatusChanged(UserStatusChangedEvent event) {
        loggingUtils.logServiceStart("UserEventPublisher", "publishUserStatusChanged", event.getUserId());

        log.info("Capturing UserStatusChangedEvent in outbox for user: {}", event.getUserId());
        outboxService.captureEvent("User", event.getUserId().toString(),
                RabbitMQConfig.USER_EXCHANGE, "user.status.changed", event);

        loggingUtils.logServiceEnd("UserEventPublisher", "publishUserStatusChanged", event.getUserId());
    }

    public void publishUserRoleChanged(UserRoleChangedEvent event) {
        loggingUtils.logServiceStart("UserEventPublisher", "publishUserRoleChanged", event.getUserId());

        log.info("Capturing UserRoleChangedEvent in outbox for user: {}", event.getUserId());
        outboxService.captureEvent("User", event.getUserId().toString(),
                RabbitMQConfig.USER_EXCHANGE, "user.role.changed", event);

        loggingUtils.logServiceEnd("UserEventPublisher", "publishUserRoleChanged", event.getUserId());
    }

    public void publishUserPasswordChanged(UserPasswordChangedEvent event) {
        loggingUtils.logServiceStart("UserEventPublisher", "publishUserPasswordChanged", event.getUserId());

        log.info("Capturing UserPasswordChangedEvent in outbox for user: {}", event.getUserId());
        outboxService.captureEvent("User", event.getUserId().toString(),
                RabbitMQConfig.USER_EXCHANGE, "user.password.changed", event);

        loggingUtils.logServiceEnd("UserEventPublisher", "publishUserPasswordChanged", event.getUserId());
    }

    public void publishUserLogin(UserLoginEvent event) {
        loggingUtils.logServiceStart("UserEventPublisher", "publishUserLogin", event.getUserId());

        log.info("Capturing UserLoginEvent in outbox for user: {}", event.getUserId());
        outboxService.captureEvent("User", event.getUserId().toString(),
                RabbitMQConfig.USER_EXCHANGE, "user.login", event);

        loggingUtils.logServiceEnd("UserEventPublisher", "publishUserLogin", event.getUserId());
    }
}
