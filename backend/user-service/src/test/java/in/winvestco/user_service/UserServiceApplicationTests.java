package in.winvestco.user_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import org.springframework.boot.test.mock.mockito.MockBean;
import in.winvestco.common.messaging.idempotency.ProcessedEventRepository;
import in.winvestco.common.messaging.outbox.OutboxRepository;
import in.winvestco.common.util.LoggingUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@SpringBootTest
@ActiveProfiles("test")
class UserServiceApplicationTests {

    @MockBean
    private ProcessedEventRepository processedEventRepository;

    @MockBean
    private OutboxRepository outboxRepository;

    @MockBean
    private LoggingUtils loggingUtils;

    @MockBean
    private RabbitTemplate rabbitTemplate;

    @Test
    void contextLoads() {
        // Verify that the Spring context loads successfully
    }
}
