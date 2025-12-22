package in.winvestco.payment_service.service;

import in.winvestco.common.enums.PaymentStatus;
import in.winvestco.payment_service.messaging.PaymentEventPublisher;
import in.winvestco.payment_service.model.Payment;
import in.winvestco.payment_service.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Scheduler to expire pending payments that have exceeded their TTL
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentExpiryScheduler {

    private final PaymentRepository paymentRepository;
    private final PaymentEventPublisher eventPublisher;

    /**
     * Run to check for expired payments - triggered via RabbitMQ
     */
    @Transactional
    public void expirePayments() {
        List<PaymentStatus> statuses = List.of(
                PaymentStatus.INITIATED,
                PaymentStatus.PENDING);

        List<Payment> expiredPayments = paymentRepository.findExpiredPayments(
                statuses,
                Instant.now());

        if (expiredPayments.isEmpty()) {
            return;
        }

        log.info("Found {} expired payments to process", expiredPayments.size());

        for (Payment payment : expiredPayments) {
            try {
                payment.markExpired();
                paymentRepository.save(payment);
                eventPublisher.publishPaymentExpired(payment);
                log.info("Expired payment: {}", payment.getId());
            } catch (Exception e) {
                log.error("Failed to expire payment {}: {}", payment.getId(), e.getMessage());
            }
        }
    }
}
