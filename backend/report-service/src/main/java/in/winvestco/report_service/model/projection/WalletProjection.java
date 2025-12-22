package in.winvestco.report_service.model.projection;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Wallet Projection - Current wallet state projection.
 * Updated from FundsDepositedEvent, FundsWithdrawnEvent, FundsLockedEvent, FundsReleasedEvent.
 */
@Entity
@Table(name = "wallet_projections")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class WalletProjection implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "wallet_id", nullable = false)
    private Long walletId;

    @Column(name = "available_balance", precision = 18, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal availableBalance = BigDecimal.ZERO;

    @Column(name = "locked_balance", precision = 18, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal lockedBalance = BigDecimal.ZERO;

    @Column(name = "currency", length = 3, nullable = false)
    @Builder.Default
    private String currency = "INR";

    @Column(name = "last_updated_at", nullable = false)
    @Builder.Default
    private Instant lastUpdatedAt = Instant.now();

    /**
     * Get total balance
     */
    public BigDecimal getTotalBalance() {
        return availableBalance.add(lockedBalance);
    }
}
