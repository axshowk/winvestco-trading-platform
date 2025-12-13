package in.winvestco.funds_service.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import in.winvestco.common.enums.LockStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
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
 * FundsLock entity - tracks funds locked for pending orders.
 * 
 * Lifecycle:
 * 1. LOCKED - funds locked when order is placed
 * 2. RELEASED - funds returned to available on cancel/reject
 * 3. SETTLED - funds used for trade execution
 */
@Entity
@Table(name = "funds_locks", indexes = {
    @Index(name = "idx_locks_wallet_id", columnList = "wallet_id"),
    @Index(name = "idx_locks_order_id", columnList = "order_id", unique = true),
    @Index(name = "idx_locks_status", columnList = "status")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class FundsLock implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    @EqualsAndHashCode.Include
    private Long id;

    @NotNull
    @Column(name = "wallet_id", nullable = false)
    private Long walletId;

    @NotBlank
    @Size(max = 100)
    @Column(name = "order_id", nullable = false, unique = true, length = 100)
    private String orderId;

    @NotNull
    @Positive
    @Column(name = "amount", precision = 18, scale = 4, nullable = false)
    private BigDecimal amount;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private LockStatus status = LockStatus.LOCKED;

    @Size(max = 500)
    @Column(name = "reason", length = 500)
    private String reason;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    /**
     * Release the lock (return funds to available)
     */
    public void release(String releaseReason) {
        this.status = LockStatus.RELEASED;
        this.reason = releaseReason;
    }

    /**
     * Settle the lock (funds used for trade)
     */
    public void settle(String settleReason) {
        this.status = LockStatus.SETTLED;
        this.reason = settleReason;
    }
}
