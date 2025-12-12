package in.winvestco.user_service.service;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import in.winvestco.common.event.UserCreatedEvent;
import in.winvestco.common.event.UserUpdatedEvent;
import in.winvestco.common.event.UserStatusChangedEvent;
import in.winvestco.common.event.UserRoleChangedEvent;
import in.winvestco.common.event.UserPasswordChangedEvent;
import in.winvestco.common.event.UserLoginEvent;
import in.winvestco.common.util.LoggingUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final LoggingUtils loggingUtils;

    @Value("${app.rabbitmq.exchange:user.exchange}")
    private String exchange;

    @Value("${app.rabbitmq.routingKey:user.created}")
    private String userCreatedRoutingKey;

    @Value("${app.rabbitmq.routingKey.user.updated:user.updated}")
    private String userUpdatedRoutingKey;

    @Value("${app.rabbitmq.routingKey.user.status.changed:user.status.changed}")
    private String userStatusChangedRoutingKey;

    @Value("${app.rabbitmq.routingKey.user.role.changed:user.role.changed}")
    private String userRoleChangedRoutingKey;

    @Value("${app.rabbitmq.routingKey.user.password.changed:user.password.changed}")
    private String userPasswordChangedRoutingKey;

    @Value("${app.rabbitmq.routingKey.user.login:user.login}")
    private String userLoginRoutingKey;

    public void publishUserCreated(UserCreatedEvent event) {
        loggingUtils.logServiceStart("UserEventPublisher", "publishUserCreated", event.getUserId());

        rabbitTemplate.convertAndSend(exchange, userCreatedRoutingKey, event);

        loggingUtils.logServiceEnd("UserEventPublisher", "publishUserCreated", event.getUserId());
    }

    public void publishUserUpdated(UserUpdatedEvent event) {
        loggingUtils.logServiceStart("UserEventPublisher", "publishUserUpdated", event.getUserId());

        rabbitTemplate.convertAndSend(exchange, userUpdatedRoutingKey, event);

        loggingUtils.logServiceEnd("UserEventPublisher", "publishUserUpdated", event.getUserId());
    }

    public void publishUserStatusChanged(UserStatusChangedEvent event) {
        loggingUtils.logServiceStart("UserEventPublisher", "publishUserStatusChanged", event.getUserId());

        rabbitTemplate.convertAndSend(exchange, userStatusChangedRoutingKey, event);

        loggingUtils.logServiceEnd("UserEventPublisher", "publishUserStatusChanged", event.getUserId());
    }

    public void publishUserRoleChanged(UserRoleChangedEvent event) {
        loggingUtils.logServiceStart("UserEventPublisher", "publishUserRoleChanged", event.getUserId());

        rabbitTemplate.convertAndSend(exchange, userRoleChangedRoutingKey, event);

        loggingUtils.logServiceEnd("UserEventPublisher", "publishUserRoleChanged", event.getUserId());
    }

    public void publishUserPasswordChanged(UserPasswordChangedEvent event) {
        loggingUtils.logServiceStart("UserEventPublisher", "publishUserPasswordChanged", event.getUserId());

        rabbitTemplate.convertAndSend(exchange, userPasswordChangedRoutingKey, event);

        loggingUtils.logServiceEnd("UserEventPublisher", "publishUserPasswordChanged", event.getUserId());
    }

    public void publishUserLogin(UserLoginEvent event) {
        loggingUtils.logServiceStart("UserEventPublisher", "publishUserLogin", event.getUserId());

        rabbitTemplate.convertAndSend(exchange, userLoginRoutingKey, event);

        loggingUtils.logServiceEnd("UserEventPublisher", "publishUserLogin", event.getUserId());
    }
}
