package in.winvestco.funds_service.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import in.winvestco.common.enums.WalletStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Wallet entity representing a user's cash balance.
 * Each user has exactly one wallet.
 * 
 * Balance tracking:
 * - availableBalance: funds available for trading/withdrawal
 * - lockedBalance: funds locked for pending orders
 * - totalBalance = availableBalance + lockedBalance (derived)
 */
@Entity
@Table(name = "wallets", indexes = {
    @Index(name = "idx_wallets_user_id", columnList = "user_id", unique = true)
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Wallet implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    @EqualsAndHashCode.Include
    private Long id;

    @NotNull
    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "available_balance", precision = 18, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal availableBalance = BigDecimal.ZERO;

    @Column(name = "locked_balance", precision = 18, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal lockedBalance = BigDecimal.ZERO;

    @Column(name = "currency", length = 3, nullable = false)
    @Builder.Default
    private String currency = "INR";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private WalletStatus status = WalletStatus.ACTIVE;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    /**
     * Get total balance (available + locked)
     */
    public BigDecimal getTotalBalance() {
        BigDecimal available = availableBalance != null ? availableBalance : BigDecimal.ZERO;
        BigDecimal locked = lockedBalance != null ? lockedBalance : BigDecimal.ZERO;
        return available.add(locked);
    }

    /**
     * Check if wallet has sufficient available balance
     */
    public boolean hasSufficientBalance(BigDecimal amount) {
        return availableBalance != null && availableBalance.compareTo(amount) >= 0;
    }

    /**
     * Credit available balance
     */
    public void credit(BigDecimal amount) {
        this.availableBalance = this.availableBalance.add(amount);
    }

    /**
     * Debit available balance
     */
    public void debit(BigDecimal amount) {
        this.availableBalance = this.availableBalance.subtract(amount);
    }

    /**
     * Lock funds from available to locked balance
     */
    public void lockFunds(BigDecimal amount) {
        this.availableBalance = this.availableBalance.subtract(amount);
        this.lockedBalance = this.lockedBalance.add(amount);
    }

    /**
     * Unlock funds from locked back to available balance
     */
    public void unlockFunds(BigDecimal amount) {
        this.lockedBalance = this.lockedBalance.subtract(amount);
        this.availableBalance = this.availableBalance.add(amount);
    }

    /**
     * Settle locked funds (deduct from locked without returning to available)
     */
    public void settleFunds(BigDecimal amount) {
        this.lockedBalance = this.lockedBalance.subtract(amount);
    }
}
