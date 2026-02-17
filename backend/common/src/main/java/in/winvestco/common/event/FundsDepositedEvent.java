package in.winvestco.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Event emitted when funds are deposited/credited to a wallet.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class FundsDepositedEvent extends BaseEvent {
    private static final long serialVersionUID = 1L;

    private Long userId;
    private Long walletId;
    private BigDecimal amount;
    private BigDecimal balanceBefore;
    private BigDecimal newBalance;
    private String referenceId; // Transaction reference
    private String depositMethod; // "BANK_TRANSFER", "UPI", "CARD", etc.
    private Instant depositedAt;
}
