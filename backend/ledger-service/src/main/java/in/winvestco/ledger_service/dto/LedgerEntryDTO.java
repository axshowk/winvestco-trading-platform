package in.winvestco.ledger_service.dto;

import in.winvestco.common.enums.LedgerEntryType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * DTO for ledger entry information (read-only)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LedgerEntryDTO {
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
