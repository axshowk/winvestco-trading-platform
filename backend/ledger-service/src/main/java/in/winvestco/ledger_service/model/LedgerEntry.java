package in.winvestco.ledger_service.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import in.winvestco.common.enums.LedgerEntryType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.Immutable;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * LedgerEntry entity - IMMUTABLE audit trail for all money movements.
 * 
 * THIS IS THE SOURCE OF TRUTH FOR ALL FINANCIAL DATA.
 * 
 * CRITICAL DESIGN:
 * - @Immutable annotation prevents Hibernate from issuing UPDATEs
 * - No setters for critical fields after construction
 * - Only createdAt, NO updatedAt field
 * - All changes are tracked as new entries
 * 
 * Every money movement creates a new ledger entry for:
 * - Audit compliance
 * - Regulatory requirements
 * - Reconciliation
 */
@Entity
@Immutable  // Hibernate annotation - prevents UPDATE operations
@Table(name = "ledger_entries", indexes = {
    @Index(name = "idx_ledger_wallet_id", columnList = "wallet_id"),
    @Index(name = "idx_ledger_reference", columnList = "reference_id, reference_type"),
    @Index(name = "idx_ledger_created_at", columnList = "created_at"),
    @Index(name = "idx_ledger_entry_type", columnList = "entry_type")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)  // JPA requirement, but protected
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class LedgerEntry implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    @EqualsAndHashCode.Include
    private Long id;

    @NotNull
    @Column(name = "wallet_id", nullable = false, updatable = false)
    private Long walletId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 20, updatable = false)
    private LedgerEntryType entryType;

    @NotNull
    @Column(name = "amount", precision = 18, scale = 4, nullable = false, updatable = false)
    private BigDecimal amount;

    @NotNull
    @Column(name = "balance_before", precision = 18, scale = 4, nullable = false, updatable = false)
    private BigDecimal balanceBefore;

    @NotNull
    @Column(name = "balance_after", precision = 18, scale = 4, nullable = false, updatable = false)
    private BigDecimal balanceAfter;

    @Size(max = 100)
    @Column(name = "reference_id", length = 100, updatable = false)
    private String referenceId;

    @Size(max = 50)
    @Column(name = "reference_type", length = 50, updatable = false)
    private String referenceType;

    @Size(max = 500)
    @Column(name = "description", length = 500, updatable = false)
    private String description;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // ==============================================
    // NO updatedAt field - IMMUTABLE
    // NO setters - IMMUTABLE after creation
    // ==============================================

    /**
     * Factory method to create a new ledger entry.
     * This is the ONLY way to create entries.
     */
    public static LedgerEntry create(
            Long walletId,
            LedgerEntryType entryType,
            BigDecimal amount,
            BigDecimal balanceBefore,
            BigDecimal balanceAfter,
            String referenceId,
            String referenceType,
            String description) {
        
        return LedgerEntry.builder()
                .walletId(walletId)
                .entryType(entryType)
                .amount(amount)
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .referenceId(referenceId)
                .referenceType(referenceType)
                .description(description)
                .build();
    }
}
