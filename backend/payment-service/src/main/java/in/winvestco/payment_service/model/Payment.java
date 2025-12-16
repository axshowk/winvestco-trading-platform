package in.winvestco.payment_service.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import in.winvestco.common.enums.PaymentMethod;
import in.winvestco.common.enums.PaymentProvider;
import in.winvestco.common.enums.PaymentStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Payment entity for Razorpay integration.
 * 
 * Lifecycle: CREATED → INITIATED → PENDING → SUCCESS/FAILED/EXPIRED
 */
@Entity
@Table(name = "payments", indexes = {
    @Index(name = "idx_payment_user_id", columnList = "user_id"),
    @Index(name = "idx_payment_status", columnList = "status"),
    @Index(name = "idx_payment_razorpay_order", columnList = "razorpay_order_id"),
    @Index(name = "idx_payment_razorpay_payment", columnList = "razorpay_payment_id"),
    @Index(name = "idx_payment_expires_at", columnList = "expires_at")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Payment implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    @EqualsAndHashCode.Include
    private Long id;

    @NotNull
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "wallet_id")
    private Long walletId;

    @NotNull
    @Positive
    @Column(name = "amount", precision = 18, scale = 4, nullable = false)
    private BigDecimal amount;

    @Size(max = 3)
    @Column(name = "currency", length = 3)
    @Builder.Default
    private String currency = "INR";

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.CREATED;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 20)
    private PaymentMethod paymentMethod;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 20)
    @Builder.Default
    private PaymentProvider provider = PaymentProvider.RAZORPAY;

    // Razorpay specific fields
    @Size(max = 100)
    @Column(name = "razorpay_order_id", length = 100)
    private String razorpayOrderId;

    @Size(max = 100)
    @Column(name = "razorpay_payment_id", length = 100)
    private String razorpayPaymentId;

    @Size(max = 255)
    @Column(name = "razorpay_signature", length = 255)
    private String razorpaySignature;

    // Transaction metadata
    @Size(max = 100)
    @Column(name = "receipt", length = 100)
    private String receipt;

    @Size(max = 500)
    @Column(name = "description", length = 500)
    private String description;

    @Size(max = 500)
    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Size(max = 50)
    @Column(name = "error_code", length = 50)
    private String errorCode;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    // ==================== State Transition Methods ====================

    /**
     * Transition to INITIATED state after Razorpay order creation
     */
    public void initiate(String razorpayOrderId, Instant expiresAt) {
        if (this.status != PaymentStatus.CREATED) {
            throw new IllegalStateException("Cannot initiate payment from status: " + this.status);
        }
        this.status = PaymentStatus.INITIATED;
        this.razorpayOrderId = razorpayOrderId;
        this.expiresAt = expiresAt;
    }

    /**
     * Transition to PENDING state when user is redirected to payment page
     */
    public void markPending() {
        if (this.status != PaymentStatus.INITIATED) {
            throw new IllegalStateException("Cannot mark pending from status: " + this.status);
        }
        this.status = PaymentStatus.PENDING;
    }

    /**
     * Transition to SUCCESS state after webhook verification
     */
    public void markSuccess(String paymentId, String signature, PaymentMethod method) {
        if (this.status != PaymentStatus.PENDING && this.status != PaymentStatus.INITIATED) {
            throw new IllegalStateException("Cannot mark success from status: " + this.status);
        }
        this.status = PaymentStatus.SUCCESS;
        this.razorpayPaymentId = paymentId;
        this.razorpaySignature = signature;
        this.paymentMethod = method;
    }

    /**
     * Transition to FAILED state
     */
    public void markFailed(String reason, String errorCode) {
        if (this.status == PaymentStatus.SUCCESS || this.status == PaymentStatus.EXPIRED) {
            throw new IllegalStateException("Cannot mark failed from status: " + this.status);
        }
        this.status = PaymentStatus.FAILED;
        this.failureReason = reason;
        this.errorCode = errorCode;
    }

    /**
     * Transition to EXPIRED state
     */
    public void markExpired() {
        if (this.status == PaymentStatus.SUCCESS || this.status == PaymentStatus.FAILED) {
            throw new IllegalStateException("Cannot mark expired from status: " + this.status);
        }
        this.status = PaymentStatus.EXPIRED;
        this.failureReason = "Payment expired without completion";
    }

    /**
     * Check if payment is in a terminal state
     */
    public boolean isTerminal() {
        return this.status == PaymentStatus.SUCCESS 
            || this.status == PaymentStatus.FAILED 
            || this.status == PaymentStatus.EXPIRED;
    }

    /**
     * Check if payment can be expired
     */
    public boolean canExpire() {
        return (this.status == PaymentStatus.INITIATED || this.status == PaymentStatus.PENDING)
            && this.expiresAt != null 
            && Instant.now().isAfter(this.expiresAt);
    }
}
