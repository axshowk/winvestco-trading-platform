package in.winvestco.funds_service.dto;

import in.winvestco.common.enums.LedgerEntryType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for creating a new ledger entry via ledger-service.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateLedgerEntryRequest {

    @NotNull(message = "Wallet ID is required")
    private Long walletId;

    @NotNull(message = "Entry type is required")
    private LedgerEntryType entryType;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    @NotNull(message = "Balance before is required")
    private BigDecimal balanceBefore;

    @NotNull(message = "Balance after is required")
    private BigDecimal balanceAfter;

    @Size(max = 100, message = "Reference ID must not exceed 100 characters")
    private String referenceId;

    @Size(max = 50, message = "Reference type must not exceed 50 characters")
    private String referenceType;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;
}
