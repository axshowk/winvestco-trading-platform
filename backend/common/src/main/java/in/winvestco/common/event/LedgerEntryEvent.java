package in.winvestco.common.event;

import in.winvestco.common.enums.LedgerEntryType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Event published when a new ledger entry is recorded.
 * Source of truth for all financial movements.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LedgerEntryEvent {
    private Long id;
    private Long walletId;
    private LedgerEntryType entryType;
    private BigDecimal amount;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private String referenceId;
    private String referenceType;
    private String description;
    private Instant createdAt;
}
