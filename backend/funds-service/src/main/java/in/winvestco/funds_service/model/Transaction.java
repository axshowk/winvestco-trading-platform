package in.winvestco.funds_service.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import in.winvestco.common.enums.TransactionStatus;
import in.winvestco.common.enums.TransactionType;
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
 * Transaction entity - tracks deposits and withdrawals.
 * 
 * Supports async confirmation workflow:
 * 1. PENDING - initiated by user
 * 2. PROCESSING - being processed by payment gateway
 * 3. COMPLETED - successfully completed
 * 4. FAILED - processing failed
 * 5. CANCELLED - cancelled by user/system
 */
@Entity
@Table(name = "transactions", indexes = {
    @Index(name = "idx_tx_wallet_id", columnList = "wallet_id"),
    @Index(name = "idx_tx_external_ref", columnList = "external_reference"),
    @Index(name = "idx_tx_status", columnList = "status"),
    @Index(name = "idx_tx_created_at", columnList = "created_at")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Transaction implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    @EqualsAndHashCode.Include
    private Long id;

    @NotNull
    @Column(name = "wallet_id", nullable = false)
    private Long walletId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 20)
    private TransactionType transactionType;

    @NotNull
    @Positive
    @Column(name = "amount", precision = 18, scale = 4, nullable = false)
    private BigDecimal amount;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private TransactionStatus status = TransactionStatus.PENDING;

    @Size(max = 200)
    @Column(name = "external_reference", length = 200)
    private String externalReference;

    @Size(max = 500)
    @Column(name = "description", length = 500)
    private String description;

    @Size(max = 500)
    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    /**
     * Mark transaction as processing
     */
    public void markProcessing() {
        this.status = TransactionStatus.PROCESSING;
    }

    /**
     * Mark transaction as completed
     */
    public void complete() {
        this.status = TransactionStatus.COMPLETED;
    }

    /**
     * Mark transaction as failed
     */
    public void fail(String reason) {
        this.status = TransactionStatus.FAILED;
        this.failureReason = reason;
    }

    /**
     * Mark transaction as cancelled
     */
    public void cancel(String reason) {
        this.status = TransactionStatus.CANCELLED;
        this.failureReason = reason;
    }
}
