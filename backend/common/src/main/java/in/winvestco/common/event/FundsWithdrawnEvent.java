package in.winvestco.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Event emitted when funds are withdrawn/debited from a wallet.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FundsWithdrawnEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long userId;
    private Long walletId;
    private BigDecimal amount;
    private BigDecimal balanceBefore;
    private BigDecimal newBalance;
    private String referenceId; // Transaction reference
    private String withdrawalMethod; // "BANK_TRANSFER", "UPI", etc.
    private String bankAccountLast4; // Last 4 digits of destination account
    private Instant withdrawnAt;
}
