package in.winvestco.payment_service.repository;

import in.winvestco.common.enums.PaymentStatus;
import in.winvestco.payment_service.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Payment entity
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /**
     * Find payment by Razorpay order ID
     */
    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);

    /**
     * Find payment by Razorpay payment ID
     */
    Optional<Payment> findByRazorpayPaymentId(String razorpayPaymentId);

    /**
     * Find payments by user ID ordered by creation date
     */
    List<Payment> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Find expired payments (for scheduler)
     */
    @Query("SELECT p FROM Payment p WHERE p.status IN :statuses AND p.expiresAt < :now")
    List<Payment> findExpiredPayments(
        @Param("statuses") List<PaymentStatus> statuses,
        @Param("now") Instant now
    );

    /**
     * Find payments by status
     */
    List<Payment> findByStatus(PaymentStatus status);

    /**
     * Find payments by user and status
     */
    List<Payment> findByUserIdAndStatus(Long userId, PaymentStatus status);

    /**
     * Count payments by user and status
     */
    long countByUserIdAndStatus(Long userId, PaymentStatus status);
}
