package in.winvestco.common.event;

import in.winvestco.common.enums.LedgerEntryType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Event published when a new ledger entry is recorded.
 * Source of truth for all financial movements.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class LedgerEntryEvent extends BaseEvent {
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
