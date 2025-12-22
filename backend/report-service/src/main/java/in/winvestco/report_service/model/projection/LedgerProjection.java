package in.winvestco.report_service.model.projection;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Ledger Projection - Local projection of financial transactions.
 * Populated from FundsDepositedEvent, FundsWithdrawnEvent, etc.
 */
@Entity
@Table(name = "ledger_projections", indexes = {
    @Index(name = "idx_ledger_proj_user_id", columnList = "user_id"),
    @Index(name = "idx_ledger_proj_wallet_id", columnList = "wallet_id"),
    @Index(name = "idx_ledger_proj_created_at", columnList = "created_at"),
    @Index(name = "idx_ledger_proj_entry_type", columnList = "entry_type"),
    @Index(name = "idx_ledger_proj_user_date", columnList = "user_id, created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class LedgerProjection implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "wallet_id", nullable = false)
    private Long walletId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "entry_type", nullable = false, length = 30)
    private String entryType;

    @Column(name = "amount", precision = 18, scale = 4, nullable = false)
    private BigDecimal amount;

    @Column(name = "balance_before", precision = 18, scale = 4, nullable = false)
    private BigDecimal balanceBefore;

    @Column(name = "balance_after", precision = 18, scale = 4, nullable = false)
    private BigDecimal balanceAfter;

    @Column(name = "reference_id", length = 100)
    private String referenceId;

    @Column(name = "reference_type", length = 50)
    private String referenceType;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
